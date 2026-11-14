extends Node3D

var player: CharacterBody3D
var pivot: Node3D
var camera: Camera3D
var camera_third := true
var yaw := 0.0
var pitch := -0.15
var move_touch := Vector2.ZERO
var look_touch_id := -1
var move_touch_id := -1
var move_start := Vector2.ZERO
var look_last := Vector2.ZERO
var jump_requested := false
var hud: CanvasLayer
var player_mesh: MeshInstance3D
var supplies := 0
var supplies_label: Label

func _ready() -> void:
	_build_world()
	_build_player()
	_build_hud()
	_build_supplies()

func _box(pos: Vector3, size: Vector3, color: Color, solid := true) -> Node3D:
	var mesh := MeshInstance3D.new()
	var shape := BoxMesh.new()
	shape.size = size
	mesh.mesh = shape
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	mesh.material_override = material
	mesh.position = pos
	add_child(mesh)
	if solid:
		var body := StaticBody3D.new()
		body.position = pos
		var collider := CollisionShape3D.new()
		var box := BoxShape3D.new()
		box.size = size
		collider.shape = box
		body.add_child(collider)
		add_child(body)
	return mesh

func _build_world() -> void:
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-55, 35, 0)
	sun.light_energy = 1.2
	add_child(sun)
	var environment := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.45, 0.67, 0.85)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.75, 0.8, 0.85)
	env.ambient_light_energy = 0.65
	environment.environment = env
	add_child(environment)
	_box(Vector3(0, -0.5, 0), Vector3(240, 1, 240), Color(0.26, 0.4, 0.23))
	_box(Vector3(0, 0.015, 0), Vector3(7, 0.03, 220), Color(0.21, 0.23, 0.25), false)
	for i in range(18):
		var z := -85.0 + i * 10.0
		var side := -1.0 if i % 2 == 0 else 1.0
		_box(Vector3(side * (13 + i % 3 * 5), 2, z), Vector3(6, 4, 7), Color(0.35 + 0.02 * (i % 4), 0.35, 0.32))
	for i in range(48):
		var x := float((i * 37) % 180 - 90)
		var z := float((i * 53) % 180 - 90)
		if abs(x) < 8:
			continue
		_box(Vector3(x, 1.5, z), Vector3(0.6, 3, 0.6), Color(0.32, 0.22, 0.12))
		_box(Vector3(x, 3.5, z), Vector3(3, 3, 3), Color(0.13, 0.32, 0.15), false)

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.position = Vector3(0, 2, 8)
	add_child(player)
	var collision := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.38
	capsule.height = 1.8
	collision.shape = capsule
	collision.position.y = 0.9
	player.add_child(collision)
	var body := MeshInstance3D.new()
	var model := CapsuleMesh.new()
	model.radius = 0.38
	model.height = 1.8
	body.mesh = model
	body.position.y = 0.9
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.1, 0.55, 0.8)
	body.material_override = mat
	player.add_child(body)
	player_mesh = body
	pivot = Node3D.new()
	pivot.position.y = 1.55
	player.add_child(pivot)
	camera = Camera3D.new()
	camera.current = true
	pivot.add_child(camera)
	_update_camera()

func _update_camera() -> void:
	camera.position = Vector3(0, 0.35, 4.0) if camera_third else Vector3(0, 0, -0.15)
	player_mesh.visible = camera_third

func _build_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	hud.add_child(root)
	var title := Label.new()
	title.text = "NO LIMITS: SURVIVAL 3D  |  OPEN WORLD PROTOTYPE"
	title.position = Vector2(20, 16)
	title.add_theme_font_size_override("font_size", 23)
	root.add_child(title)
	supplies_label = Label.new()
	supplies_label.text = "SUPPLIES: 0 / 12"
	supplies_label.position = Vector2(20, 55)
	supplies_label.add_theme_font_size_override("font_size", 23)
	root.add_child(supplies_label)
	var button := Button.new()
	button.text = "FPS / TPS"
	button.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	button.position = Vector2(-185, 20)
	button.size = Vector2(165, 70)
	button.pressed.connect(func(): camera_third = not camera_third; _update_camera())
	root.add_child(button)
	var jump := Button.new()
	jump.text = "JUMP"
	jump.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	jump.position = Vector2(-160, -125)
	jump.size = Vector2(140, 80)
	jump.pressed.connect(func(): jump_requested = true)
	root.add_child(jump)
	var hint := Label.new()
	hint.text = "LEFT: MOVE     RIGHT: LOOK"
	hint.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	hint.position = Vector2(20, -42)
	root.add_child(hint)

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			if event.position.x < get_viewport().get_visible_rect().size.x * 0.5 and move_touch_id == -1:
				move_touch_id = event.index
				move_start = event.position
			elif look_touch_id == -1:
				look_touch_id = event.index
				look_last = event.position
		else:
			if event.index == move_touch_id:
				move_touch_id = -1
				move_touch = Vector2.ZERO
			if event.index == look_touch_id:
				look_touch_id = -1
	if event is InputEventScreenDrag:
		if event.index == move_touch_id:
			move_touch = (event.position - move_start) / 90.0
			move_touch = move_touch.limit_length(1.0)
		if event.index == look_touch_id:
			var delta := event.position - look_last
			yaw -= delta.x * 0.004
			pitch = clamp(pitch - delta.y * 0.004, -1.1, 0.75)
			look_last = event.position
	if event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_RIGHT):
		yaw -= event.relative.x * 0.004
		pitch = clamp(pitch - event.relative.y * 0.004, -1.1, 0.75)

func _physics_process(delta: float) -> void:
	player.rotation.y = yaw
	pivot.rotation.x = pitch
	var input_vector := move_touch
	input_vector += Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
	input_vector = input_vector.limit_length(1.0)
	var direction := player.global_transform.basis * Vector3(input_vector.x, 0, input_vector.y)
	player.velocity.x = direction.x * 6.0
	player.velocity.z = direction.z * 6.0
	if not player.is_on_floor():
		player.velocity.y -= 18.0 * delta
	elif jump_requested or Input.is_action_just_pressed("ui_accept"):
		player.velocity.y = 7.0
	jump_requested = false
	player.move_and_slide()

func _build_supplies() -> void:
	for i in range(12):
		var pickup := Area3D.new()
		var x := float((i * 19) % 38 - 19)
		var z := float((i * 23) % 85 - 42)
		if abs(x) < 5:
			x += 8.0
		pickup.position = Vector3(x, 0.7, z)
		var visual := MeshInstance3D.new()
		var shape := BoxMesh.new()
		shape.size = Vector3(0.65, 0.65, 0.65)
		visual.mesh = shape
		var material := StandardMaterial3D.new()
		material.albedo_color = Color(1.0, 0.75, 0.15)
		visual.material_override = material
		pickup.add_child(visual)
		var collision := CollisionShape3D.new()
		var box := BoxShape3D.new()
		box.size = Vector3(1.3, 1.5, 1.3)
		collision.shape = box
		pickup.add_child(collision)
		add_child(pickup)
		pickup.body_entered.connect(_collect_supply.bind(pickup))

func _collect_supply(body: Node3D, pickup: Area3D) -> void:
	if body != player:
		return
	supplies += 1
	supplies_label.text = "SUPPLIES: %d / 12" % supplies
	pickup.queue_free()
