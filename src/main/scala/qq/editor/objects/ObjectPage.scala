package qq.editor.objects

import de.ust.skill.common.scala.api;
import scala.collection.mutable;
import scala.swing;
import qq.util.Swing.HBoxD;
import qq.util.Swing.HBoxT;
import scala.swing.Swing.HGlue
import scala.swing.Label
import scala.swing.Action
import scala.swing.Button

/**
 * A page displaying data about a (set of) object, with an optional search results page on the
 * left and an option graph representation on the right
 */
class ObjectPage(file0: qq.editor.File, settings0: qq.editor.Settings) extends qq.editor.Page(file0, settings0) {

  /** search panel visible*/
  def searchVisible: Boolean = searchVisibleModel.isSelected()
  val searchVisibleModel = new javax.swing.JToggleButton.ToggleButtonModel()
  searchVisibleModel.setSelected(true)

  /** graph visible */
  def graphVisible: Boolean = graphVisibleModel.isSelected()
  val graphVisibleModel = new javax.swing.JToggleButton.ToggleButtonModel()
  graphVisibleModel.setSelected(true)

  class View(
      val obj: api.SkillObject,
      /** objects that are explicitly requested to be shown */
      val showAlso: mutable.HashSet[api.SkillObject] = new mutable.HashSet()) {
    def this(obj0: api.SkillObject, showAlso0: Iterable[api.SkillObject]) =
      this(obj0, new mutable.HashSet[api.SkillObject]() { this ++= showAlso0 })
  }

  /** the object that is currently shown */
  var currentView: View = null

  /** previously shown types (for back navigation) */
  val previousView: mutable.Stack[View] = new mutable.Stack()

  /** previously previously shown types :) (for forward navigation) */
  val nextView: mutable.Stack[View] = new mutable.Stack()

  /**
   * a function that does something with an object. When not null, the page will be
   * in an object selection mode and contain a `select' button that will cause
   * this continuation to be called with the current object
   */
  val objectSelectionContinuation: api.SkillObject ⇒ Unit = null
  val objectSelectionCancelContinuation: Unit ⇒ Unit = null
  /** Title for object selection */
  val objectSelectionTitle: String = ""

  /** the graph panel */
  var graph: ObjectGraph[_] = null

  /** show an object (internal, for goTo, goBack, goForward) */
  private def _goTo(v: View): Unit = {
    currentView = v
    title = file.idOfObj(v.obj)

    objEdit.contents.clear()
    objEdit.contents += new TopObjectEdit(this, v.obj)

    objGraph.contents.clear()
    graph = new ObjectGraph(this, v.obj)
    objGraph.contents += graph

    goBack.enabled = previousView.length > 0
    goForward.enabled = nextView.length > 0
  }

  /** run a query (will open first object found) */
  def find(q: String): Unit = {
    objSearch.queryText := q
    objSearch.searchAction()
  }
  /** show a root object and potential other objects and update navigation */
  def goTo(v: View): Unit = {
    nextView.clear()
    if (currentView != null && v.obj != currentView.obj) previousView.push(currentView)
    _goTo(v)
  }
  /** show an object */
  def goTo(o: api.SkillObject): Unit = {
    find(file.idOfObj(o))
  }
  /** show previously shown type */
  val goBack = new swing.Action("Back") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("alt LEFT"))
    mnemonic = swing.event.Key.B.id
    icon = new qq.icons.BackIcon(true)
    smallIcon = new qq.icons.BackIcon(true, true)
    enabled = false
    override def apply() = {
      if (previousView.length > 0) {
        val t = previousView.pop()
        nextView.push(currentView)
        _goTo(t)
      }
    }
  }
  /** show next type */
  val goForward = new swing.Action("Forward") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("alt RIGHT"))
    mnemonic = swing.event.Key.F.id
    icon = new qq.icons.ForwardIcon(true)
    smallIcon = new qq.icons.ForwardIcon(true, true)
    enabled = false
    override def apply() = {
      if (nextView.length > 0) {
        val t = nextView.pop()
        previousView.push(currentView)
        _goTo(t)
      }
    }
  }

  val toggleSearchVisible = new swing.Action("Show Search") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl S"))
    mnemonic = swing.event.Key.S.id
    override def apply() = {
      updateVisibility
    }
  }
  val toggleGraphVisible = new swing.Action("Show Graph") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl G"))
    mnemonic = swing.event.Key.G.id
    override def apply() = {
      updateVisibility
    }
  }

  val showTypeOfThisObject = new swing.Action("Show Type of Current Object") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl T"))
    mnemonic = swing.event.Key.T.id
    override def apply() = {
      qq.editor.Main.newTypeTab(file.s(currentView.obj.getTypeName))
    }
  }

  val deleteThisObject = new swing.Action("Delete Current Object") {
    accelerator = Some(javax.swing.KeyStroke.getKeyStroke("ctrl D"))
    mnemonic = swing.event.Key.D.id
    override def apply() = {
      new qq.editor.UserDeleteObject(
        file, file.s(currentView.obj.getTypeName).asInstanceOf[api.Access[api.SkillObject]], currentView.obj)
    }
  }

  /* menu entries for the main window */
  override def viewMenuItems = Seq(
    new swing.MenuItem(goBack),
    new swing.MenuItem(goForward),
    new swing.CheckMenuItem("") {
      action = toggleSearchVisible
      peer.setModel(searchVisibleModel)
    },
    new swing.CheckMenuItem("") {
      action = toggleGraphVisible
      peer.setModel(graphVisibleModel)
    })
  override def typeMenuItems = Seq(
    new swing.MenuItem(showTypeOfThisObject))
  override def objectMenuItems = Seq(
    new swing.MenuItem(deleteThisObject))
  /* the layout */
  val toolBar = qq.util.Swing.HBoxD(
    new swing.Button(goBack) {
      text = ""
      icon = new qq.icons.BackIcon(true)
      disabledIcon = new qq.icons.BackIcon(false)
    },
    new swing.Button(goForward) {
      text = ""
      icon = new qq.icons.ForwardIcon(true)
      disabledIcon = new qq.icons.ForwardIcon(false)
    },
    new swing.ToggleButton("") {
      action = toggleSearchVisible
      peer.setModel(searchVisibleModel)
    },
    new swing.ToggleButton("") {
      action = toggleGraphVisible
      peer.setModel(graphVisibleModel)
    },
    new swing.Button(swing.Action("Redraw") {
      if (graph != null) {
        graph.clampedNodes.clear()
        graph.updateLayout
        graph.repaint()
      }
    }),
    scala.swing.Swing.HGlue,
    new swing.Button(swing.Action("ps to clipboard") {
      if (graph != null) {
        val clipboard = java.awt.Toolkit.getDefaultToolkit.getSystemClipboard
        val sel = new java.awt.datatransfer.StringSelection(graph.graph.toPs(graph.size))
        clipboard.setContents(sel, sel)
      }
    }),
    new swing.Button(swing.Action("random obj") {
      /* pick random object, expand all neighbours */
      import de.ust.skill.common.scala.internal.StoragePool
      val nobj = file.s.map { x ⇒ x.asInstanceOf[StoragePool[_, _]].staticInstances.size }.sum
      var pick = math.floor(math.random * nobj).toInt
      for (pool ← file.s) {
        val p = pool.asInstanceOf[StoragePool[_, _]]
        if (0 <= pick && pick < p.staticInstances.size) {
          file.typeSettings(pool).expanded ++= pool.fields.map(Seq(_))
          for (s ← file.superTypes(pool)) {
            file.typeSettings(s).expanded ++= s.fields.map(Seq(_))
          }
          goTo(p.staticInstances.take(pick).next.asInstanceOf[api.SkillObject])
          pick -= 1 //
        }
        pick -= p.staticInstances.size
      }

    }))
  val objSearch = new SearchResults(this)
  val objEdit = HBoxD()
  val objGraph = HBoxT()

  val mainContent = HBoxD()

  def updateVisibility: Unit = {
    mainContent.contents.clear()
    if (searchVisible) {
      if (graphVisible) {
        mainContent.contents += new swing.SplitPane(swing.Orientation.Vertical) {
          contents_=(objSearch, new swing.SplitPane(swing.Orientation.Vertical) {
            contents_=(objEdit, objGraph)
          })
        }
      } else {
        mainContent.contents += new swing.SplitPane(swing.Orientation.Vertical) {
          contents_=(objSearch, objEdit)
        }
      }
    } else {
      if (graphVisible) {
        mainContent.contents += new swing.SplitPane(swing.Orientation.Vertical) {
          contents_=(objEdit, objGraph)
        }
      } else {
        mainContent.contents += objEdit
      }
    }
    mainContent.revalidate()
  }
  val objSelectTitle = HBoxD()
  val objSelectButtons = HBoxD()
  objSelectTitle.visible = false
  objSelectButtons.visible = false
  val objSelectErrorLabel = new swing.Label("-") { foreground = java.awt.Color.red; visible = false }

  /**
   * use this page to select an object. Show title on top, buttons on bottom. When
   *  the user presses the button, the on… handler is called and the page is closed
   */
  def select(title: String,
             onAccept: api.SkillObject ⇒ Unit,
             onCancel: Unit ⇒ Unit): Unit = {
    val accept = Action("Ok") {
      try {
        onAccept(currentView.obj)
        tabbedPane.removePage(index)
      } catch {
        case e: Exception ⇒
          objSelectErrorLabel.text = qq.util.binding.ValidationExceptionMessage(e)
          objSelectErrorLabel.visible = true
      }
    }
    val cancel = Action("Cancel") {
      try {
        onCancel(())
        tabbedPane.removePage(index)
      } catch {
        case e: Exception ⇒
          objSelectErrorLabel.text = qq.util.binding.ValidationExceptionMessage(e)
          objSelectErrorLabel.visible = true
      }
    }
    objSelectTitle.contents.clear()
    objSelectTitle.contents ++= Seq(HGlue, new Label(s"<html><h1>$title</h1></html>"), HGlue)
    objSelectTitle.visible = true
    objSelectButtons.contents.clear()
    objSelectButtons.contents ++= Seq(HGlue, objSelectErrorLabel, HGlue, new Button(accept), new Button(cancel))
    objSelectButtons.visible = true
  }

  updateVisibility
  title = "Objects"
  content = qq.util.Swing.VBoxD(toolBar, objSelectTitle, mainContent, objSelectButtons)
}