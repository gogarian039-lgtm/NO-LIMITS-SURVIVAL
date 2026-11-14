extends SceneTree

func _initialize() -> void:
	var sdk: String = OS.get_environment("GODOT_ANDROID_SDK_PATH")
	var java: String = OS.get_environment("GODOT_JAVA_SDK_PATH")
	if not DirAccess.dir_exists_absolute(sdk) or not DirAccess.dir_exists_absolute(java):
		push_error("Missing Android SDK or Java SDK")
		quit(1)
		return
	var settings: EditorSettings = EditorInterface.get_editor_settings()
	settings.set_setting("export/android/android_sdk_path", sdk)
	settings.set_setting("export/android/java_sdk_path", java)
	print("Saved Android SDK and Java SDK paths in Godot editor settings")
	quit()
