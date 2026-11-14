extends Node3D

# First playable Godot 4 scene. Geometry is deliberately lightweight for mobile.
var player: CharacterBody3D
var camera: Camera3D
var hud: Label
var joystick_center := Vector2.ZERO
var move_touch := -1
var look_touch := -1
var joystick := Vector2.ZERO
var last_look := Vector2.ZERO
var yaw := 0.0
var pitch := 0.0
var wood := 0
var stone := 0
var health := 100.0
var hunger := 100.0
var thirst := 100.0
var items: Array[Node3D] = []
var rng := RandomNumberGenerator.new()
var world_time := 0.0

func _ready() -> void:
    rng.seed = 20261114
    _build_environment()
    _build_player()
    _build_hud()
    Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)

func _material(color: Color, roughness := 0.9) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = color
    m.roughness = roughness
    return m

func _mesh_box(pos: Vector3, size: Vector3, color: Color, collision := false) -> MeshInstance3D:
    var mesh := BoxMesh.new()
    mesh.size = size
    var node := MeshInstance3D.new()
    node.mesh = mesh
    node.material_override = _material(color)
    node.position = pos
    add_child(node)
    if collision:
        var body := StaticBody3D.new()
        var shape := CollisionShape3D.new()
        var box := BoxShape3D.new()
        box.size = size
        shape.shape = box
        node.add_child(body)
        body.add_child(shape)
    return node

func _build_environment() -> void:
    var environment := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.52, 0.72, 0.89)
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.72, 0.79, 0.86)
    env.ambient_light_energy = 0.7
    env.fog_enabled = true
    env.fog_light_color = Color(0.69, 0.78, 0.79)
    env.fog_density = 0.009
    environment.environment = env
    add_child(environment)
    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-48, -28, 0)
    sun.light_energy = 1.3
    sun.shadow_enabled = true
    add_child(sun)
    _mesh_box(Vector3(0, -0.7, 0), Vector3(240, 1.2, 240), Color(0.24, 0.38, 0.18), true)
    # Water is a visual river strip. Swimming is not yet implemented.
    _mesh_box(Vector3(25, -0.065, 0), Vector3(10, 0.06, 240), Color(0.13, 0.37, 0.51))
    for i in range(260):
        var x := rng.randf_range(-105.0, 105.0)
        var z := rng.randf_range(-105.0, 105.0)
        if absf(x - 25.0) < 8.0 or Vector2(x, z).length() < 9.0:
            continue
        _tree(Vector3(x, 0, z), rng.randf_range(0.75, 1.65))
    for i in range(100):
        var x := rng.randf_range(-105.0, 105.0)
        var z := rng.randf_range(-105.0, 105.0)
        if absf(x - 25.0) < 7.0 or Vector2(x, z).length() < 5.0:
            continue
        var size := rng.randf_range(0.3, 1.2)
        _mesh_box(Vector3(x, size * 0.25, z), Vector3(size, size * 0.5, size), Color(0.39, 0.41, 0.37))
    # A landmark cabin, with a door opening.
    _mesh_box(Vector3(-16, 2.2, -19), Vector3(8, 4.4, 0.4), Color(0.34, 0.22, 0.13), true)
    _mesh_box(Vector3(-16, 2.2, -27), Vector3(8, 4.4, 0.4), Color(0.34, 0.22, 0.13), true)
    _mesh_box(Vector3(-20, 2.2, -23), Vector3(0.4, 4.4, 8), Color(0.34, 0.22, 0.13), true)
    _mesh_box(Vector3(-12, 2.2, -25.5), Vector3(0.4, 4.4, 3), Color(0.34, 0.22, 0.13), true)
    _mesh_box(Vector3(-12, 2.2, -20.5), Vector3(0.4, 4.4, 3), Color(0.34, 0.22, 0.13), true)
    _mesh_box(Vector3(-16, 4.7, -23), Vector3(9, 0.5, 9), Color(0.19, 0.19, 0.18), true)
    for i in range(24):
        var p := Vector3(rng.randf_range(-35, 35), 0.35, rng.randf_range(-35, 35))
        if p.distance_to(Vector3.ZERO) < 3.0:
            continue
        var resource := _mesh_box(p, Vector3(0.6, 0.7, 0.6), Color(0.55, 0.48, 0.31) if i % 2 == 0 else Color(0.48, 0.51, 0.53))
        resource.set_meta("resource_type", "WOOD" if i % 2 == 0 else "STONE")
        items.append(resource)

func _tree(p: Vector3, scale_factor: float) -> void:
    var trunk := CylinderMesh.new()
    trunk.top_radius = 0.20 * scale_factor
    trunk.bottom_radius = 0.32 * scale_factor
    trunk.height = 3.6 * scale_factor
    var stem := MeshInstance3D.new()
    stem.mesh = trunk
    stem.material_override = _material(Color(0.29, 0.19, 0.11))
    stem.position = p + Vector3(0, trunk.height * 0.5, 0)
    add_child(stem)
    for j in range(3):
        var crown := MeshInstance3D.new()
        var cone := CylinderMesh.new()
        cone.top_radius = 0.05
        cone.bottom_radius = (1.8 - j * 0.35) * scale_factor
        cone.height = 2.7 * scale_factor
        crown.mesh = cone
        crown.material_override = _material(Color(0.12 + j * 0.018, 0.29 + j * 0.025, 0.14))
        crown.position = p + Vector3(0, (3.2 + j * 1.15) * scale_factor, 0)
        add_child(crown)

func _build_player() -> void:
    player = CharacterBody3D.new()
    player.position = Vector3(0, 1.3, 3)
    add_child(player)
    var capsule := CapsuleShape3D.new()
    capsule.radius = 0.35
    capsule.height = 1.8
    var collision := CollisionShape3D.new()
    collision.shape = capsule
    collision.position.y = 0.9
    player.add_child(collision)
    camera = Camera3D.new()
    camera.position.y = 1.65
    camera.current = true
    camera.fov = 75
    player.add_child(camera)

func _build_hud() -> void:
    var layer := CanvasLayer.new()
    add_child(layer)
    hud = Label.new()
    hud.position = Vector2(18, 16)
    hud.add_theme_font_size_override("font_size", 22)
    layer.add_child(hud)
    var use_button := Button.new()
    use_button.text = "USE / PICK UP"
    use_button.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
    use_button.offset_left = -210
    use_button.offset_top = -95
    use_button.offset_right = -18
    use_button.offset_bottom = -24
    use_button.pressed.connect(_pickup)
    layer.add_child(use_button)
    var hint := Label.new()
    hint.text = "LEFT: MOVE     RIGHT: LOOK     USE: COLLECT"
    hint.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
    hint.offset_left = 18
    hint.offset_top = -42
    hint.offset_right = 640
    hint.offset_bottom = -12
    layer.add_child(hint)

func _physics_process(delta: float) -> void:
    world_time += delta
    hunger = maxf(0, hunger - delta * 0.06)
    thirst = maxf(0, thirst - delta * 0.10)
    var input_vec := Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
    if joystick.length() > 0.1:
        input_vec = joystick
    var direction := (Basis(Vector3.UP, yaw) * Vector3(input_vec.x, 0, input_vec.y)).normalized()
    var sprint := Input.is_key_pressed(KEY_SHIFT)
    player.velocity.x = direction.x * (7.0 if sprint else 4.2)
    player.velocity.z = direction.z * (7.0 if sprint else 4.2)
    player.velocity.y -= 19.6 * delta
    player.move_and_slide()
    player.rotation.y = yaw
    camera.rotation.x = pitch
    hud.text = "NO LIMITS SURVIVAL 3D\nHP: %d   FOOD: %d   WATER: %d\nWOOD: %d   STONE: %d" % [int(health), int(hunger), int(thirst), wood, stone]

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventKey and event.pressed:
        if event.keycode == KEY_E:
            _pickup()
        elif event.keycode == KEY_ESCAPE:
            Input.set_mouse_mode(Input.MOUSE_MODE_VISIBLE)
    if event is InputEventMouseMotion and Input.get_mouse_mode() == Input.MOUSE_MODE_CAPTURED:
        yaw -= event.relative.x * 0.0025
        pitch = clampf(pitch - event.relative.y * 0.0025, -1.3, 1.3)
    if event is InputEventScreenTouch:
        if event.pressed:
            if event.position.x < get_viewport().get_visible_rect().size.x * 0.45 and move_touch == -1:
                move_touch = event.index
                joystick_center = event.position
            elif look_touch == -1:
                look_touch = event.index
                last_look = event.position
        else:
            if event.index == move_touch:
                move_touch = -1
                joystick = Vector2.ZERO
            if event.index == look_touch:
                look_touch = -1
    if event is InputEventScreenDrag:
        if event.index == move_touch:
            joystick = ((event.position - joystick_center) / 90.0).limit_length(1)
        elif event.index == look_touch:
            var motion: Vector2 = event.position - last_look
            yaw -= motion.x * 0.004
            pitch = clampf(pitch - motion.y * 0.004, -1.3, 1.3)
            last_look = event.position

func _pickup() -> void:
    var closest: Node3D = null
    var best := 2.8
    for item in items:
        if not is_instance_valid(item):
            continue
        var distance := player.global_position.distance_to(item.global_position)
        if distance < best:
            best = distance
            closest = item
    if closest != null:
        if closest.get_meta("resource_type") == "WOOD":
            wood += 1
        else:
            stone += 1
        items.erase(closest)
        closest.queue_free()
