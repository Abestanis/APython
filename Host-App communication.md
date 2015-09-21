#Communication Protocol Template

The first proposal of how the communication between the host and the app should happen:

* Host: [This project](https://github.com/Abestanis/APython)
* App: Python code with a [wrapper](https://github.com/Abestanis/APython-PyApp) around it built by our [PyToApk-Tool](https://github.com/Abestanis/APython-PyToApk)

*Version 0 (Edit 3)*

1. The App starts an intent with the action `com.python.pythonhost.PYTHON_APP_GET_EXECUTION_INFO`. This way, if the user has another Python Host installed, he can choose which one he wants to use. The intent contains some information, mainly the used protocol version, a package- and class-name and the list of required modules.
2. The host checks the required modules and downloads them as needed using [pip](https://pip.pypa.io/en/stable/).
3. The host starts an intent which is constructed from the given package- and class-name. To start this intent the Python host needs to have the permission `com.python.permission.PYTHONHOST`. The Python host fills the intent with a list of paths to C-libraries.
4. The Python App loads these libraries and executes a special native function (`startApp`) which starts the python interpreter which executes the App's Python code.
