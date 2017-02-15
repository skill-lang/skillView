package qq.util

class Vector(val x: Float,val y: Float) {
  def isZero(): Boolean = x == 0 && y == 0
  def isFinite(): Boolean = !x.isInfinite() && ! y.isInfinite() && !x.isNaN() && ! y.isNaN()
  def +(r: Vector): Vector = new Vector(x+r.x, y+r.y) 
  def -(r: Vector): Vector = new Vector(x-r.x, y-r.y) 
  def unary_-(): Vector = new Vector(-x, -y)
  def *(r: Vector): Float = x * r.x + y * r.y
  def *(r: Float): Vector = new Vector(x*r, y*r)
  def /(r: Float): Vector = new Vector(x/r, y/r)
  def abs: Float = math.sqrt(x*x + y*y).toFloat
  def direction: Double = math.atan2(y, x)
  def norm: Vector = this / this.abs
  /** a copy of this shortened when necessary so that it is no longer then r */
  def min(r: Float): Vector = if (this.abs < r) this else this.norm * r
  /** a copy of this lengthened when necessary so that it is no shorter then r. may point in any direction if this == (0,0) */
  def max(r: Float): Vector = if (this.abs > r) this else if (this.isZero) Vector.fromPolar(r, 2 * math.Pi * math.random) else this.norm * r
  /** the component of this that is parallel to r. a.parallelTo(b) + a.orthogonalTo(b) = a */
  def parallelTo(r: Vector): Vector = {
    val r̂  = r.norm
    r̂  * (r̂  * this)
  }
  def orthogonalTo(r: Vector) : Vector = this - this.parallelTo(r)  
  
  override def toString = s"[$x, $y]"
  
}

object Vector {
  def fromPolar(r: Float, φ: Double) = new Vector((r * math.cos(φ)).toFloat, r * math.sin(φ).toFloat)
}