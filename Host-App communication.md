#Communication Protocol Template

The first proposal of how the communication between the host and the app should happen:

* Host: [This project](https://github.com/Abestanis/APython)
* App: Python code with a [wrapper](https://github.com/Abestanis/APython-PyApp) around it built by our [PyToApk-Tool](https://github.com/Abestanis/APython-PyToApk)

*Version 0 (Edit 2)*

1. App starts an intent with the action "com.python.pythonhost.PYTHON_APP_EXECUTE". This way, if the user has another Python Host installed, he can chose which one he wants to use. The intent contains some information, mainly the used protocol version and the list of required modules.
2. Host binds to the Apps communication service. This is only possible if the host has the permission "com.python.permission.PYTHONHOST".
3. The host processes the list of required modules (which would go into the [site-packages folder](https://docs.python.org/2/install/#how-installation-works) on other platforms) and downloads them as needed. We should download from [here](https://pypi.python.org/pypi).
4. Host requests the .pyo files throught the communication service.
5. [Optional (if the App does not have .pyo files)] Host requests the .py through the communication service.
7. [Optional] Host converts them to .pyo files and returns them to the App which saves them.
8. Host spawns a new process and executes the python code
9. If the python code requests an Android specific action, the request is send to the App which needs to have the required permission to execute the action (e.g. get the GPS location)
