package ru.kaelesty.madprojects

import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = IosAppBridge().mainViewController()
