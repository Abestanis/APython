#Communication Protocol Template

The first proposal of how the communication between the host and the app should happen:

* Host: [This project](https://github.com/Abestanis/APython)
* App: Python code with a [wrapper](https://github.com/Abestanis/APython-PyApp) around it built by our [PyToApk-Tool](https://github.com/Abestanis/APython-PyToApk)

*Version 0.1*

1. App sends a [broadcast](http://developer.android.com/guide/components/intents-filters.html).
2. Host returns an interface (something from [here](http://developer.android.com/guide/components/bound-services.html) and/or [here](http://developer.android.com/training/articles/security-tips.html#IPC)).
3. App checks the version of python, host and communication protocol.
4. App checks Host for permission 'python.interpreter'. This way only Apps with this permission can access the features of the Python app and Androids security system does not get broken.
5. App sends a 'requires' command with a list of required modules (which would go into the [site-packages folder](https://docs.python.org/2/install/#how-installation-works) on other platforms) to the Host, which downloads them as needed. We should download from [here](https://pypi.python.org/pypi).
6. [Optional (mostly for debug purposes)] App grants Host access to .py files.
7. [Optional (mostly for debug purposes)] Host converts them to .pyo files and returns them to the App which saves them.
8. App grants Host access to .pyo files.
9. Host spawns a new process and executes the python code
10. If the python code requests an Android specific action, the request is send to the App which needs to have the required permission to execute the action (e.g. get the GPS location)
