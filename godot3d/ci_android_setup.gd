extends EditorScript

func _run() -> void:
	var settings: EditorSettings = get_editor_interface().get_editor_settings()
	var sdk: String = OS.get_environment("GODOT_ANDROID_SDK_PATH")
	var java: String = OS.get_environment("GODOT_JAVA_SDK_PATH")
	print("CI Android SDK: ", sdk)
	print("CI Java SDK: ", java)
	assert(DirAccess.dir_exists_absolute(sdk))
	assert(DirAccess.dir_exists_absolute(java))
	settings.set_setting("export/android/android_sdk_path", sdk)
	settings.set_setting("export/android/java_sdk_path", java)
	settings.save()
	print("CI saved Android editor settings")
