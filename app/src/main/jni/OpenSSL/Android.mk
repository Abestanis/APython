LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := openSSL
FILE_LIST := $(wildcard $(LOCAL_PATH)/source/ssl/*.c) $(wildcard $(LOCAL_PATH)/source/crypto/*.c) $(wildcard $(LOCAL_PATH)/source/crypto/*/*.c) #$(LOCAL_PATH)/../Python/source/Modules/_ssl.c
LOCAL_SRC_FILES := $(FILE_LIST:$(LOCAL_PATH)/%=%)
EXCLUDED_FILES := ssl/ssl_task.c \
                  ssl/ssltest.c \
                  ssl/heartbeat_test.c \
                  crypto/aes/aes_x86core.c \
                  crypto/bf/bf_opts.c \
                  crypto/bf/bf_cbc.c \
                  crypto/bf/bftest.c \
                  crypto/bf/bfspeed.c \
                  crypto/bn/bnspeed.c \
                  crypto/bn/bntest.c \
                  crypto/bn/divtest.c \
                  crypto/bn/expspeed.c \
                  crypto/bn/exptest.c \
                  crypto/bn/rsaz_exp.c \
                  crypto/cast/cast_spd.c \
                  crypto/cast/castopts.c \
                  crypto/cast/casttest.c \
                  crypto/LPdir_nyi.c \
                  crypto/LPdir_unix.c \
                  crypto/LPdir_vms.c \
                  crypto/LPdir_win.c \
                  crypto/LPdir_win32.c \
                  crypto/LPdir_wince.c \
                  crypto/constant_time_test.c \
                  crypto/o_dir_test.c \
                  crypto/armcap.c \
                  crypto/ppccap.c \
                  crypto/s390xcap.c \
                  crypto/sparcv9cap.c \
                  crypto/bio/bss_rtcp.c \
                  crypto/bn/exp.c \
                  crypto/conf/cnf_save.c \
                  crypto/conf/test.c \
                  crypto/des/des_opts.c \
                  crypto/des/read_pwd.c \
                  crypto/des/des.c \
                  crypto/des/destest.c \
                  crypto/des/rpw.c \
                  crypto/des/ncbc_enc.c \
                  crypto/des/speed.c \
                  crypto/dh/dhtest.c \
                  crypto/dh/p1024.c \
                  crypto/dh/p192.c \
                  crypto/dh/p512.c \
                  crypto/dsa/dsagen.c \
                  crypto/dsa/dsatest.c \
                  crypto/ec/ecp_nistz256_table.c \
                  crypto/ec/ecp_nistz256.c \
                  crypto/ec/ectest.c \
                  crypto/ecdh/ecdhtest.c \
                  crypto/ecdsa/ecdsatest.c \
                  crypto/engine/enginetest.c \
                  crypto/evp/e_dsa.c \
                  crypto/evp/evp_extra_test.c \
                  crypto/evp/evp_test.c \
                  crypto/hmac/hmactest.c \
                  crypto/idea/idea_spd.c \
                  crypto/idea/ideatest.c \
                  crypto/jpake/jpake.c \
                  crypto/jpake/jpake_err.c \
                  crypto/jpake/jpaketest.c \
                  crypto/lhash/lh_test.c \
                  crypto/md2/md2.c \
                  crypto/md2/md2_dgst.c \
                  crypto/md2/md2_one.c \
                  crypto/md2/md2test.c \
                  crypto/md4/md4.c \
                  crypto/md4/md4test.c \
                  crypto/md5/md5.c \
                  crypto/md5/md5test.c \
                  crypto/mdc2/mdc2test.c \
                  crypto/pkcs7/pk7_enc.c \
                  crypto/pqueue/pq_test.c \
                  crypto/rand/randtest.c \
                  crypto/rc2/rc2speed.c \
                  crypto/rc2/rc2test.c \
                  crypto/rc2/tab.c \
                  crypto/rc4/rc4.c \
                  crypto/rc4/rc4speed.c \
                  crypto/rc4/rc4test.c \
                  crypto/rc5/rc5_ecb.c \
                  crypto/rc5/rc5_enc.c \
                  crypto/rc5/rc5_skey.c \
                  crypto/rc5/rc5cfb64.c \
                  crypto/rc5/rc5ofb64.c \
                  crypto/rc5/rc5speed.c \
                  crypto/rc5/rc5test.c \
                  crypto/ripemd/rmd160.c \
                  crypto/ripemd/rmdtest.c \
                  crypto/rsa/rsa_test.c \
                  crypto/sha/sha.c \
                  crypto/sha/sha1.c \
                  crypto/sha/sha1test.c \
                  crypto/sha/sha256t.c \
                  crypto/sha/sha512t.c \
                  crypto/sha/shatest.c \
                  crypto/srp/srptest.c \
                  crypto/store/str_err.c \
                  crypto/store/str_lib.c \
                  crypto/store/str_mem.c \
                  crypto/store/str_meth.c \
                  crypto/threads/mttest.c \
                  crypto/whrlpool/wp_test.c \
                  crypto/x509v3/v3conf.c \
                  crypto/x509v3/v3prin.c \
                  crypto/x509v3/tabtest.c \
                  crypto/x509v3/v3nametest.c \

EXCLUDED_FILES := $(addprefix source/, $(EXCLUDED_FILES))
LOCAL_SRC_FILES := $(filter-out $(EXCLUDED_FILES), $(FILE_LIST:$(LOCAL_PATH)/%=%))

LOCAL_CFLAGS = -DOPENSSL_NO_HW -DOPENSSL_NO_GOST
LOCAL_C_INCLUDES := $(LOCAL_PATH)/source/include $(LOCAL_PATH)/source/crypto $(dir $(wildcard $(LOCAL_PATH)/source/crypto/*/*.h))
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/source/include
LOCAL_SHARED_LIBRARIES := pythonPatch #python3.4

#LOCAL_SHORT_COMMANDS = true
#include $(BUILD_SHARED_LIBRARY)

LOCAL_SRC_FILES := libopenSSL.so
include $(PREBUILT_SHARED_LIBRARY)
