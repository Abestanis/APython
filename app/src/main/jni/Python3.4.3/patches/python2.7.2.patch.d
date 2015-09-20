--- Python-2.7.2.orig/Python-2.7.2/Lib/distutils/sysconfig.py
+++ Python-2.7.2.new/Python-2.7.2/Lib/distutils/sysconfig.py
@@ -148,7 +148,7 @@ def customize_compiler(compiler):
     Mainly needed on Unix, so we can plug in the information that
     varies across Unices and is stored in Python's Makefile.
     """
-    if compiler.compiler_type == "unix":
+    if compiler.compiler_type == "unix" and sys.platform != "android": # TODO: Edited here
         (cc, cxx, opt, cflags, ccshared, ldshared, so_ext) = \
             get_config_vars('CC', 'CXX', 'OPT', 'CFLAGS',
                             'CCSHARED', 'LDSHARED', 'SO')
@@ -358,6 +358,18 @@ _config_vars = None
 def _init_posix():
     """Initialize the module as appropriate for POSIX systems."""
     g = {}
+    global _config_vars # TODO: Edited here
+    if sys.platform == 'android':
+        g['LIBDEST'] = get_python_lib(plat_specific=0, standard_lib=1)
+        g['BINLIBDEST'] = get_python_lib(plat_specific=1, standard_lib=1)
+
+        #g['INCLUDEPY'] = get_python_inc(plat_specific=0)
+
+        g['SO'] = '.so'
+        g['VERSION'] = get_python_version().replace(".", "")
+        #g['BINDIR'] = os.path.dirname(os.path.abspath(sys.executable))
+        _config_vars = g
+        return
     # load the installed Makefile:
     try:
         filename = get_makefile_filename()
@@ -430,7 +442,7 @@ def _init_posix():
             g['LDSHARED'] = ("%s -L%s/lib -lpython%s" %
                              (linkerscript, PREFIX, get_python_version()))
 
-    global _config_vars
+    #global _config_vars
     _config_vars = g
 

--- Python-2.7.2.orig/Python-2.7.2/Lib/sysconfig.py
+++ Python-2.7.2.new/Python-2.7.2/Lib/sysconfig.py
@@ -276,6 +276,16 @@ def _get_makefile_filename():

 def _init_posix(vars):
     """Initialize the module as appropriate for POSIX systems."""
+    if sys.platform == 'android':
+        vars['LIBDEST'] = get_path('stdlib')
+        vars['BINLIBDEST'] = get_path('platstdlib')
+
+        #vars['INCLUDEPY'] = get_python_inc(plat_specific=0)
+
+        vars['SO'] = '.so'
+        vars['VERSION'] = _PY_VERSION_SHORT_NO_DOT
+        vars['BINDIR'] = os.path.dirname(_safe_realpath(sys.executable))
+        return
     # load the installed Makefile:
     makefile = _get_makefile_filename()
     try:

--- Python-2.7.2.orig/Python-2.7.2/Modules/_localemodule.c
+++ Python-2.7.2.new/Python-2.7.2/Modules/_localemodule.c
@@ -206,7 +206,7 @@ static PyObject*
 PyLocale_localeconv(PyObject* self)
 {
     PyObject* result;
-    struct lconv *l;
+    //struct lconv *l; //TODO: Edited here
     PyObject *x;
 
     result = PyDict_New();
@@ -214,52 +214,52 @@ PyLocale_localeconv(PyObject* self)
         return NULL;
 
     /* if LC_NUMERIC is different in the C library, use saved value */
-    l = localeconv();
+    //l = localeconv();
 
     /* hopefully, the localeconv result survives the C library calls
        involved herein */
 
 #define RESULT_STRING(s)\
-    x = PyString_FromString(l->s);\
+    x = PyString_FromString(s);/*l->s);*/\
     if (!x) goto failed;\
     PyDict_SetItemString(result, #s, x);\
     Py_XDECREF(x)
 
 #define RESULT_INT(i)\
-    x = PyInt_FromLong(l->i);\
+    x = PyInt_FromLong(i);/*l->i);*/\
     if (!x) goto failed;\
     PyDict_SetItemString(result, #i, x);\
     Py_XDECREF(x)
 
     /* Numeric information */
-    RESULT_STRING(decimal_point);
-    RESULT_STRING(thousands_sep);
-    x = copy_grouping(l->grouping);
+    RESULT_STRING(".");//decimal_point);
+    RESULT_STRING("");//thousands_sep);
+    x = copy_grouping("");//l->grouping);
     if (!x)
         goto failed;
     PyDict_SetItemString(result, "grouping", x);
     Py_XDECREF(x);
 
     /* Monetary information */
-    RESULT_STRING(int_curr_symbol);
-    RESULT_STRING(currency_symbol);
-    RESULT_STRING(mon_decimal_point);
-    RESULT_STRING(mon_thousands_sep);
-    x = copy_grouping(l->mon_grouping);
+    RESULT_STRING("");//int_curr_symbol);
+    RESULT_STRING("");//currency_symbol);
+    RESULT_STRING("");//mon_decimal_point);
+    RESULT_STRING("");//mon_thousands_sep);
+    x = copy_grouping("");//l->mon_grouping);
     if (!x)
         goto failed;
     PyDict_SetItemString(result, "mon_grouping", x);
     Py_XDECREF(x);
-    RESULT_STRING(positive_sign);
-    RESULT_STRING(negative_sign);
-    RESULT_INT(int_frac_digits);
-    RESULT_INT(frac_digits);
-    RESULT_INT(p_cs_precedes);
-    RESULT_INT(p_sep_by_space);
-    RESULT_INT(n_cs_precedes);
-    RESULT_INT(n_sep_by_space);
-    RESULT_INT(p_sign_posn);
-    RESULT_INT(n_sign_posn);
+    RESULT_STRING("");//positive_sign);
+    RESULT_STRING("");//negative_sign);
+    RESULT_INT(CHAR_MAX);//int_frac_digits);
+    RESULT_INT(CHAR_MAX);//frac_digits);
+    RESULT_INT(CHAR_MAX);//p_cs_precedes);
+    RESULT_INT(CHAR_MAX);//p_sep_by_space);
+    RESULT_INT(CHAR_MAX);//n_cs_precedes);
+    RESULT_INT(CHAR_MAX);//n_sep_by_space);
+    RESULT_INT(CHAR_MAX);//p_sign_posn);
+    RESULT_INT(CHAR_MAX);//n_sign_posn);
     return result;
 
   failed:
--- Python-2.7.2.orig/Python-2.7.2/Modules/posixmodule.c
+++ Python-2.7.2.new/Python-2.7.2/Modules/posixmodule.c
@@ -3787,13 +3787,13 @@ posix_openpty(PyObject *self, PyObject *noargs)
     slave_fd = open(slave_name, O_RDWR | O_NOCTTY); /* open slave */
     if (slave_fd < 0)
         return posix_error();
-#if !defined(__CYGWIN__) && !defined(HAVE_DEV_PTC)
-    ioctl(slave_fd, I_PUSH, "ptem"); /* push ptem */
-    ioctl(slave_fd, I_PUSH, "ldterm"); /* push ldterm */
-#ifndef __hpux
-    ioctl(slave_fd, I_PUSH, "ttcompat"); /* push ttcompat */
-#endif /* __hpux */
-#endif /* HAVE_CYGWIN */
+//#if !defined(__CYGWIN__) && !defined(HAVE_DEV_PTC) // TODO: Edited here
+//    ioctl(slave_fd, I_PUSH, "ptem"); /* push ptem */
+//    ioctl(slave_fd, I_PUSH, "ldterm"); /* push ldterm */
+//#ifndef __hpux
+//    ioctl(slave_fd, I_PUSH, "ttcompat"); /* push ttcompat */
+//#endif /* __hpux */
+//#endif /* HAVE_CYGWIN */
 #endif /* HAVE_OPENPTY */
 
     return Py_BuildValue("(ii)", master_fd, slave_fd);
--- Python-2.7.2.orig/Python-2.7.2/Modules/pwdmodule.c
+++ Python-2.7.2.new/Python-2.7.2/Modules/pwdmodule.c
@@ -75,7 +75,7 @@ mkpwent(struct passwd *p)
 #endif
     SETI(setIndex++, p->pw_uid);
     SETI(setIndex++, p->pw_gid);
-#ifdef __VMS
+#if 1//def __VMS// TODO: Edited here
     SETS(setIndex++, "");
 #else
     SETS(setIndex++, p->pw_gecos);
--- Python-2.7.2.orig/Python-2.7.2/Modules/termios.c
+++ Python-2.7.2.new/Python-2.7.2/Modules/termios.c
@@ -227,6 +227,7 @@ termios_tcsendbreak(PyObject *self, PyObject *args)
     return Py_None;
 }
 
+#if 0 // TODO: Edited here:
 PyDoc_STRVAR(termios_tcdrain__doc__,
 "tcdrain(fd) -> None\n\
 \n\
@@ -246,6 +247,7 @@ termios_tcdrain(PyObject *self, PyObject *args)
     Py_INCREF(Py_None);
     return Py_None;
 }
+#endif
 
 PyDoc_STRVAR(termios_tcflush__doc__,
 "tcflush(fd, queue) -> None\n\
@@ -301,8 +303,8 @@ static PyMethodDef termios_methods[] =
      METH_VARARGS, termios_tcsetattr__doc__},
     {"tcsendbreak", termios_tcsendbreak,
      METH_VARARGS, termios_tcsendbreak__doc__},
-    {"tcdrain", termios_tcdrain,
-     METH_VARARGS, termios_tcdrain__doc__},
+    //{"tcdrain", termios_tcdrain, //TODO: Edited here
+     //METH_VARARGS, termios_tcdrain__doc__},
     {"tcflush", termios_tcflush,
      METH_VARARGS, termios_tcflush__doc__},
     {"tcflow", termios_tcflow,
--- Python-2.7.2.orig/Python-2.7.2/Objects/stringlib/formatter.h
+++ Python-2.7.2.new/Python-2.7.2/Objects/stringlib/formatter.h
@@ -639,13 +639,7 @@ static void
 get_locale_info(int type, LocaleInfo *locale_info)
 {
     switch (type) {
-    case LT_CURRENT_LOCALE: {
-        struct lconv *locale_data = localeconv();
-        locale_info->decimal_point = locale_data->decimal_point;
-        locale_info->thousands_sep = locale_data->thousands_sep;
-        locale_info->grouping = locale_data->grouping;
-        break;
-    }
+    case LT_CURRENT_LOCALE:// TODO: HAD TO FIX HERE!!!
     case LT_DEFAULT_LOCALE:
         locale_info->decimal_point = ".";
         locale_info->thousands_sep = ",";
--- Python-2.7.2.orig/Python-2.7.2/Objects/stringlib/localeutil.h
+++ Python-2.7.2.new/Python-2.7.2/Objects/stringlib/localeutil.h
@@ -202,9 +202,9 @@ _Py_InsertThousandsGroupingLocale(STRINGLIB_CHAR *buffer,
                                   Py_ssize_t n_digits,
                                   Py_ssize_t min_width)
 {
-        struct lconv *locale_data = localeconv();
-        const char *grouping = locale_data->grouping;
-        const char *thousands_sep = locale_data->thousands_sep;
+        //struct lconv *locale_data = localeconv(); //TODO: Edited here
+        const char *grouping = "\3\0";//locale_data->grouping;
+        const char *thousands_sep = ",";//locale_data->thousands_sep;
 
         return _Py_InsertThousandsGrouping(buffer, n_buffer, digits, n_digits,
                                            min_width, grouping, thousands_sep);
--- Python-2.7.2.orig/Python-2.7.2/Python/bltinmodule.c
+++ Python-2.7.2.new/Python-2.7.2/Python/bltinmodule.c
@@ -19,7 +19,7 @@
 */
 #if defined(MS_WINDOWS) && defined(HAVE_USABLE_WCHAR_T)
 const char *Py_FileSystemDefaultEncoding = "mbcs";
-#elif defined(__APPLE__)
+#elif defined(__APPLE__) || 1 // TODO: Edited here
 const char *Py_FileSystemDefaultEncoding = "utf-8";
 #else
 const char *Py_FileSystemDefaultEncoding = NULL; /* use default */
--- Python-2.7.2.orig/Python-2.7.2/Python/pystrtod.c
+++ Python-2.7.2.new/Python-2.7.2/Python/pystrtod.c
@@ -126,7 +126,7 @@ _PyOS_ascii_strtod(const char *nptr, char **endptr)
 {
     char *fail_pos;
     double val = -1.0;
-    struct lconv *locale_data;
+    //struct lconv *locale_data; // TODO: FIXED HERE
     const char *decimal_point;
     size_t decimal_point_len;
     const char *p, *decimal_point_pos;
@@ -138,8 +138,9 @@ _PyOS_ascii_strtod(const char *nptr, char **endptr)
 
     fail_pos = NULL;
 
-    locale_data = localeconv();
-    decimal_point = locale_data->decimal_point;
+    // TODO: And here
+    //locale_data = localeconv();
+    decimal_point = "."; //locale_data->decimal_point;
     decimal_point_len = strlen(decimal_point);
 
     assert(decimal_point_len != 0);
@@ -375,8 +376,8 @@ PyOS_string_to_double(const char *s,
 Py_LOCAL_INLINE(void)
 change_decimal_from_locale_to_dot(char* buffer)
 {
-    struct lconv *locale_data = localeconv();
-    const char *decimal_point = locale_data->decimal_point;
+    //struct lconv *locale_data = localeconv(); // TODO: FIXED HERE
+    const char *decimal_point = ".";//locale_data->decimal_point;
 
     if (decimal_point[0] != '.' || decimal_point[1] != 0) {
         size_t decimal_point_len = strlen(decimal_point);
--- Python-2.7.2.orig/Python-2.7.2/Python/strtod.c
+++ Python-2.7.2.new/Python-2.7.2/Python/strtod.c
@@ -60,7 +60,7 @@ extern  double  atof(const char *);             /* Only called when result known
 #ifdef HAVE_ERRNO_H
 #include <errno.h>
 #endif
-extern  int     errno;
+//extern  int     errno;
 
 double strtod(char *str, char **ptr)
 {
