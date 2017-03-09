package qq.editor

import swing._;
import event._;
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import de.ust.skill.common.scala.api;

object Main extends SimpleSwingApplication {

  // ugh, buttons have background again with sys UI. And font is very small.
  // And the ugly anti-aliased fonts are still there
  // javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName())

  /** user settings */
  val settings = new Settings()

  /** the current file */
  var file: File = null

  /** event which is fired whenever file is changes (parameter is new value of file) */
  val onFileChange: qq.util.binding.Event[File] = new qq.util.binding.Event()

  /** tabs which show the actual content */
  private val tabs = new qq.util.TabbedPane()

  /** add a new tab showing type τ. τ == null allowed*/
  def newTypeTab(τ: api.Access[_ <: api.SkillObject]): qq.editor.types.TypePage = {
    val page = new qq.editor.types.TypePage(file, settings)
    tabs.addPage(page)
    if (τ != null) page.goTo(τ)
    page.show()
    page
  }

  /** add a new tab for showing objects*/
  def newObjectTab(): qq.editor.objects.ObjectPage = {
    val page = new qq.editor.objects.ObjectPage(file, settings)
    tabs.addPage(page)
    page.show()
    page
  }

  /** add a new tab showing object o. o == null allowed*/
  def newObjectTab(o: api.SkillObject): qq.editor.objects.ObjectPage = {
    val page = new qq.editor.objects.ObjectPage(file, settings)
    tabs.addPage(page)
    if (o != null) page.goTo(new page.View(o))
    page.show()
    page
  }

  /** add a new tab showing all objects of type τ */
  def newObjectTab(τ: api.Access[_]): qq.editor.objects.ObjectPage = {
    val page = new qq.editor.objects.ObjectPage(file, settings)
    tabs.addPage(page)
    if (τ != null) page.find(s"'${τ.name}'")
    page.show()
    page
  }

  private def closeFile: Unit = {
    if (file != null) {
      if (file.isModified) {
        val x = Dialog.showConfirmation(null,
          "Discard unsaved changes in " + file.fileNameOnly,
          "Discard Unsaved Changes",
          Dialog.Options.YesNo,
          Dialog.Message.Question, null)

        if (x != Dialog.Result.Yes) throw new Exception()
      }
      while (tabs.pages.length > 0) tabs.removePage(0)
      // no file.s.cles() because that will save the modification
      file = null
      onFileChange.fire(file)
    }
  }

  /** what to show after file is opened*/
  private def showDefault(): Unit = {
    /* open the first object in the smallest root pool */
    try {
      newObjectTab(file.rootTypes.filter(_.size >= 1).toSeq.sortBy(_.size).head)
    } catch {
      case _: Exception ⇒
        /* from the first non-empty pool?*/
        try {
          newObjectTab(file.s.filter(_.size >= 1).head)
        } catch {
          case _: Exception ⇒
            /* first type?*/
            try {
              newTypeTab(file.s.head)
            } catch {
              case _: Exception ⇒
              /* oh well, start empty*/
            }

        }
    }
  }
  /** open a file */
  private def open_(fileName: java.nio.file.Path) = {
    file = new File(fileName)
    undoMenuItem.action = file.undoManager.undoAction
    redoMenuItem.action = file.undoManager.redoAction
    onFileChange.fire(file)
    showDefault()
  }
  private val actOpen = new Action("Open") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl O"))
    mnemonic = swing.event.Key.O.id
    override def apply() {
      closeFile
      val fc = new FileChooser()
      fc.fileFilter = new javax.swing.filechooser.FileNameExtensionFilter("SKilL Files", "sf")
      val result = fc.showOpenDialog(null)
      if (result == FileChooser.Result.Approve) {
        try {
          open_(Paths.get(fc.selectedFile.toURI()))
        } catch {
          case e: Exception ⇒
            val x = Dialog.showConfirmation(null,
              s"""Could not open ${file.fileNameOnly}\n
              ${e.toString()}""",
              "Error saving " + file.fileNameOnly,
              Dialog.Options.Default,
              Dialog.Message.Error, null)
        }
      }
    }
  }
  private def save_() {
    // if exception here, then problem with redo of delete :( 
    file.flush()
    file.undoManager.discardAllEdits()
    /* there's no undo/redo after discardAllEdits, but the buttons are somehow not updated*/
    file.undoManager.undoAction.enabled = false
    file.undoManager.redoAction.enabled = false
    file.onModifiednessChange.fire(file.isModified)
  }
  private val actSave = new Action("Save") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl S"))
    mnemonic = swing.event.Key.S.id
    def setEnabled: Unit = {
      enabled = file != null && file.isModified
    }
    onFileChange.strong += (_ ⇒ setEnabled)
    onFileChange.strong += (file ⇒ if (file != null) file.onModifiednessChange.strong += (_ ⇒ setEnabled))
    override def apply() {
      // file is corrupted when truncate hits this problem: http://bugs.java.com/view_bug.do?bug_id=4724038
      // also when the editor makes some errors (uninitialised fields? strings in map keys)
      // therefore: at least make a backup and try to restore it (will not work in windows because the file is still locked)
      var backupMade = false
      var backupRestored = false
      try {
        val origPath = file.fileName
        val backupPath = Paths.get(file.fileName.toString() + "~")
        Files.copy(origPath, backupPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        backupMade = true
        try {
          save_
        } catch {
          case e: Exception ⇒
            try {
              java.nio.file.Files.copy(backupPath, origPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
              backupRestored = true
            } catch {
              case e: Exception ⇒ ()
            }
            throw e
        }
      } catch {
        case e: Exception ⇒
          val x = Dialog.showConfirmation(null,
            s"""Error saving ${file.fileNameOnly}: ${
              if (!backupMade) "backup could not be created"
              else if (!backupRestored) s"file is possibly corrupted, backup ${file.fileName + "~"} could not be restored"
            }\n
            ${e.toString()}""",
            "Error saving " + file.fileNameOnly,
            Dialog.Options.Default,
            Dialog.Message.Error, null)
      }
    }
  }
  private val actSaveAs = new Action("Save As …") {
    mnemonic = swing.event.Key.A.id
    onFileChange.strong += (file ⇒ enabled = file != null)
    override def apply() {
      val fc = new FileChooser()
      fc.fileFilter = new javax.swing.filechooser.FileNameExtensionFilter("SKilL Files", "sf")
      val result = fc.showSaveDialog(null)
      if (result == FileChooser.Result.Approve) {
        try {
          file.fileName = Paths.get(fc.selectedFile.toURI())
          file.s.changePath(file.fileName)
          save_
          /* TODO damn, does not work after save as anymore when empty types are removed in between */
        } catch {
          case e: Exception ⇒
            val x = Dialog.showConfirmation(null,
              s"""Could not save ${file.fileNameOnly}\n
              ${e.toString()}""",
              "Error saving " + file.fileNameOnly,
              Dialog.Options.Default,
              Dialog.Message.Error, null)
        }
      }
    }
  }
  private val actClose = new Action("Close") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl C"))
    mnemonic = swing.event.Key.C.id
    onFileChange.strong += (file ⇒ enabled = file != null)
    override def apply() {
      closeFile
    }
  }
  private val actUndoDummy = new Action("nothing to undo") {
    enabled = false
    override def apply() {}
  }
  private val actRedoDummy = new Action("nothing to redo") {
    enabled = false
    override def apply() {}
  }
  private val undoMenuItem = new MenuItem("") {
    onFileChange.strong += (file ⇒
      action = if (file != null)
        file.undoManager.undoAction
      else actUndoDummy)
  }
  private val redoMenuItem = new MenuItem("") {
    onFileChange.strong += (file ⇒
      action = if (file != null)
        file.undoManager.redoAction
      else actRedoDummy)
  }
  private val viewMenu = new Menu("View") {
    mnemonic = swing.event.Key.V
    onFileChange.strong += (_ ⇒ enabled = false) // enable when something is shown
    tabs.onPageChanged.strong += { page ⇒
      contents.clear()
      if (page != null && page.isInstanceOf[qq.editor.Page]) {
        enabled = true
        contents ++= page.asInstanceOf[qq.editor.Page].viewMenuItems
      } else {
        enabled = false
      }
    }
  }
  private val newObjectPageAction = new Action("New Object Page") {
    mnemonic = swing.event.Key.N.id
    override def apply() = {
      newObjectTab()
    }
  }
  private val newObjectPageMenuItem = new MenuItem(newObjectPageAction)
  private val objectMenu = new Menu("Object") {
    mnemonic = swing.event.Key.O
    onFileChange.strong += { file ⇒
      contents.clear()
      enabled = file != null
      if (enabled) contents += newObjectPageMenuItem
    }
    tabs.onPageChanged.strong += { page ⇒
      contents.clear()
      contents += newObjectPageMenuItem
      if (page != null && page.isInstanceOf[qq.editor.Page]) {
        val pageItems = page.asInstanceOf[qq.editor.Page].objectMenuItems
        if (pageItems.length > 0) {
          contents += new swing.Separator
          contents ++= pageItems
        }
      }
    }
  }
  private val newTypePageAction = new Action("New Type Page") {
    mnemonic = swing.event.Key.N.id
    override def apply() = {
      newTypeTab(null)
    }
  }
  private val newTypePageMenuItem = new MenuItem(newTypePageAction)
  private val defaultTypeMenuItems = Seq(
    newTypePageMenuItem,
    new qq.util.TodoMenuItem("Profiles"))
  private val typeMenu = new Menu("Type") {
    mnemonic = swing.event.Key.T
    onFileChange.strong += { file ⇒
      contents.clear()
      enabled = file != null
      if (enabled) contents ++= defaultTypeMenuItems

    }
    tabs.onPageChanged.strong += { page ⇒
      contents.clear()
      contents ++= defaultTypeMenuItems
      if (page != null && page.isInstanceOf[qq.editor.Page]) {
        val pageItems = page.asInstanceOf[qq.editor.Page].typeMenuItems
        if (pageItems.length > 0) {
          contents += new swing.Separator
          contents ++= pageItems
        }
      }
    }
  }
  private val menu = new MenuBar {
    contents ++= Seq(
      new Menu("File") {
        mnemonic = swing.event.Key.F
        contents ++= Seq(
          new MenuItem(actOpen),
          new MenuItem(actSave),
          new MenuItem(actSaveAs),
          new MenuItem(actClose))
      },
      new Menu("Edit") {
        mnemonic = swing.event.Key.E
        contents ++= Seq(undoMenuItem, redoMenuItem)
        contents += new MenuItem(Action("Properties") { settings.prefEdit.visible = true })
      },
      viewMenu, objectMenu, typeMenu)
  }
  def top = new MainFrame {
    menuBar = menu
    contents = tabs

    private def updateTitle(file: File) = {
      title = if (file != null)
        file.windowTitle
      else
        "SKilL Editor"
    }
    onFileChange.strong += updateTitle
    onFileChange.strong += (file ⇒ if (file != null) file.onModifiednessChange.strong += (_ ⇒ updateTitle(file)))

    onFileChange.fire(file)
    size = new java.awt.Dimension(640, 480)
  }

  // TODO argv[1]
  open_(Paths.get("C:\\Users\\m\\stud\\dt\\testinp\\bc.iml.sf"))

}
