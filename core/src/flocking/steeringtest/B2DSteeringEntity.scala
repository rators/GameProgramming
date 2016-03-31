package flocking.steeringtest

import com.badlogic.gdx.ai.steer.limiters.FullLimiter
import com.badlogic.gdx.ai.steer.{Steerable, SteeringAcceleration, SteeringBehavior}
import com.badlogic.gdx.ai.utils.Location
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._

/**
  * Created by rotor on 3/29/2016.
  */
class B2DSteeringEntity(val body: Body, val boundingRadius: Float) extends Steerable[Vector2] {
  var tagged: Boolean = false

  val location = Box2dLocation()

  val fullLimiter = new FullLimiter(500f, 500f, 5, 30)

  private var _behavior: SteeringBehavior[Vector2] = null

  val steerOutput: SteeringAcceleration[Vector2] = new SteeringAcceleration[Vector2](new Vector2())


  def update(delta: Float): Unit = {
    val behaviorExists = Option(_behavior)
    println(behaviorExists)
    behaviorExists match {
      case None => throw new RuntimeException("Behavior not set")
      case Some(currentBehavior) =>
        currentBehavior.calculateSteering(steerOutput)
        applySteering(delta)
    }
  }

  def applySteering(delta: Float): Unit = {
    var anyAccels = false

    steerOutput.linear.isZero match {
      case false => anyAccels = handleNonZero(delta)
      case _ => ()
    }

    anyAccels match {
      case true => handleAcceleration(delta)
      case _ => ()
    }

  }

  def handleNonZero(delta: Float): Boolean = {
    val force = steerOutput.linear.scl(delta)
    body.applyForceToCenter(force, true)
    true
  }

  def handleAcceleration(delta: Float) = {
    val velocity = body.getLinearVelocity
    val currentSpeedSquare = velocity.len2
    if (currentSpeedSquare > (getMaxLinearSpeed * getMaxLinearSpeed)) {
      body.setLinearVelocity(velocity.scl(getMaxLinearSpeed / Math.sqrt(currentSpeedSquare).toFloat))
    }
  }

  override def getLinearVelocity: Vector2 = body.getLinearVelocity

  override def isTagged: Boolean = tagged

  override def getBoundingRadius: Float = boundingRadius

  override def setTagged(tagged: Boolean): Unit = this.tagged = tagged

  override def getAngularVelocity: Float = body.getAngularVelocity

  override def getOrientation: Float = body.getAngle

  override def getPosition: Vector2 = body.getPosition

  override def angleToVector(outVector: Vector2, angle: Float): Vector2 = SteeringUtils.angleToVector(outVector, angle)

  override def vectorToAngle(vector: Vector2): Float = SteeringUtils.vectorToAngle(vector)

  override def setMaxAngularAcceleration(maxAngularAcceleration: Float): Unit = fullLimiter.setMaxAngularAcceleration(maxAngularAcceleration)

  override def getMaxAngularAcceleration: Float = fullLimiter.getMaxAngularAcceleration

  override def setMaxLinearAcceleration(maxLinearAcceleration: Float): Unit = fullLimiter.setMaxLinearAcceleration(maxLinearAcceleration)

  override def getMaxLinearSpeed: Float = fullLimiter.getMaxLinearSpeed

  override def setMaxLinearSpeed(maxLinearSpeed: Float): Unit = fullLimiter.setMaxLinearSpeed(maxLinearSpeed)

  override def getMaxLinearAcceleration: Float = fullLimiter.getMaxLinearAcceleration

  override def setMaxAngularSpeed(maxAngularSpeed: Float): Unit = fullLimiter.setMaxAngularSpeed(maxAngularSpeed)

  override def getMaxAngularSpeed: Float = fullLimiter.getMaxAngularSpeed

  override def newLocation(): Location[Vector2] = Box2dLocation()

  override def setOrientation(orientation: Float): Unit = location.setOrientation(orientation)

  override def getZeroLinearSpeedThreshold: Float = fullLimiter.getZeroLinearSpeedThreshold

  override def setZeroLinearSpeedThreshold(value: Float): Unit = fullLimiter.setZeroLinearSpeedThreshold(value)

  def behavior = _behavior

  def behavior_=(steeringBehavior: SteeringBehavior[Vector2]) = _behavior = steeringBehavior

}

object SteeringUtils {
  val PPM = 50

  def vectorToAngle(vector: Vector2) = Math.atan2(vector.x * -1d, vector.y).toFloat

  def angleToVector(outVector: Vector2, angle: Float) = {
    outVector.x = Math.sin(angle).toFloat * -1f
    outVector.y = Math.cos(angle).toFloat

    outVector
  }

  def createCircle(world: World, x: Float, y: Float, r: Float, isStatic: Boolean, canRotate: Boolean): Body = {
    val bodyDef = new BodyDef()
    bodyDef.fixedRotation = canRotate
    bodyDef.linearDamping = 10f
    bodyDef.position.set(x / PPM, y / PPM)

    isStatic match {
      case true => bodyDef.`type` = BodyDef.BodyType.StaticBody
      case false => bodyDef.`type` = BodyDef.BodyType.DynamicBody
    }

    val shape = new CircleShape()
    shape.setRadius(r / PPM)
    val fixtureDef = new FixtureDef()
    fixtureDef.shape = shape
    fixtureDef.friction = .95f
    fixtureDef.density = 1.0f

    world.createBody(bodyDef).createFixture(fixtureDef).getBody
  }

  def createBox(world: World, x: Float, y: Float, w: Float, h: Float, isStatic: Boolean, canRotate: Boolean): Body = {
    val bodyDef = new BodyDef()
    bodyDef.fixedRotation = canRotate
    bodyDef.linearDamping = 10f

    bodyDef.position.set(x / PPM, y / PPM)

    isStatic match {
      case true => bodyDef.`type` = BodyDef.BodyType.StaticBody
      case false => bodyDef.`type` = BodyDef.BodyType.DynamicBody
    }

    val shape = new PolygonShape()

    shape.setAsBox(w / PPM, h / PPM)

    val fixtureDef = new FixtureDef()
    fixtureDef.shape = shape
    fixtureDef.density = 1.0f

    world.createBody(bodyDef).createFixture(fixtureDef).getBody
  }

  def lerpToTarget(camera: Camera, target: Vector2): Unit = {
    val position = camera.position
    position.x = camera.position.x + (target.x - camera.position.x) * .1f
    position.y = camera.position.y + (target.x - camera.position.y) * .1f
    camera.position.set(position)
    camera.update()
  }
}

class Box2dLocation extends Location[Vector2] {

  var position = new Vector2()
  var orientation: Float = 0f

  override def getPosition: Vector2 = position

  override def newLocation(): Location[Vector2] = new Box2dLocation()

  override def angleToVector(outVector: Vector2, angle: Float): Vector2 = SteeringUtils.angleToVector(outVector, angle)

  override def setOrientation(orientation: Float): Unit = this.orientation = orientation

  override def vectorToAngle(vector: Vector2): Float = SteeringUtils.vectorToAngle(vector)

  override def getOrientation: Float = orientation
}

object Box2dLocation {
  def apply(): Box2dLocation = new Box2dLocation()
}
