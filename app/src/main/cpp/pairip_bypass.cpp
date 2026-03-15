#include <jni.h>
#include <dlfcn.h>
#include <string.h>
#include <android/log.h>
#include <sys/mman.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

#define TAG "GeminX-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ── Inline hook untuk arm64 ───────────────────────────────────────────────────
// Kita patch 4 instruksi pertama dari fungsi target dengan:
//   LDR x17, #8       ; load absolute address dari slot berikutnya
//   BR  x17           ; jump ke address tersebut
//   .quad <target>    ; slot address (8 bytes)
// Total: 16 bytes

static bool inline_hook(void* target_func, void* hook_func) {
    if (!target_func || !hook_func) return false;

    uintptr_t addr = (uintptr_t) target_func;

    // Page-align untuk mprotect
    uintptr_t page_start = addr & ~(getpagesize() - 1);
    size_t    page_size  = getpagesize() * 2;

    // Buat writable
    if (mprotect((void*) page_start, page_size, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        LOGE("mprotect failed for %p", target_func);
        return false;
    }

    uint32_t* code = (uint32_t*) addr;

    // LDR x17, #8  → opcode: 0x58000051
    code[0] = 0x58000051;
    // BR x17       → opcode: 0xD61F0220
    code[1] = 0xD61F0220;
    // 8-byte address slot
    *((uint64_t*)(code + 2)) = (uint64_t) hook_func;

    // Flush instruction cache
    __builtin___clear_cache((char*) addr, (char*) addr + 16);

    // Kembalikan ke read-exec saja
    mprotect((void*) page_start, page_size, PROT_READ | PROT_EXEC);

    LOGI("Inline hook installed at %p → %p", target_func, hook_func);
    return true;
}

// ── Original dlopen pointer ───────────────────────────────────────────────────
typedef void* (*dlopen_fn)(const char* filename, int flags);
static dlopen_fn original_dlopen = nullptr;

// Trampoline — simpan 16 bytes asli dari dlopen lalu jalankan setelahnya
static uint8_t dlopen_backup[16];
static bool    backup_valid = false;

// ── Hook dlopen ───────────────────────────────────────────────────────────────
static void* my_dlopen(const char* filename, int flags) {
    if (filename != nullptr && strstr(filename, "pairipcore") != nullptr) {
        LOGI("dlopen BLOCKED: %s", filename);
        // Return handle palsu yang tidak null tapi tidak valid
        // Ini mencegah crash karena null check di caller
        return (void*) 0x1;
    }

    // Panggil dlopen asli
    // Karena kita sudah patch dlopen, kita panggil via syscall-style
    // dengan restore backup dulu
    if (backup_valid) {
        // Restore bytes asli sementara
        uintptr_t addr = (uintptr_t) original_dlopen;
        uintptr_t page_start = addr & ~(getpagesize() - 1);
        mprotect((void*) page_start, getpagesize() * 2, PROT_READ | PROT_WRITE | PROT_EXEC);
        memcpy((void*) addr, dlopen_backup, 16);
        __builtin___clear_cache((char*) addr, (char*) addr + 16);
        mprotect((void*) page_start, getpagesize() * 2, PROT_READ | PROT_EXEC);

        // Panggil asli
        void* result = original_dlopen(filename, flags);

        // Re-install hook
        inline_hook((void*) original_dlopen, (void*) my_dlopen);

        return result;
    }

    return nullptr;
}

// ── Install hook ──────────────────────────────────────────────────────────────
static void install_pairip_bypass() {
    LOGI("Installing Pairip native bypass...");

    // Cari dlopen di libdl.so
    void* libdl = dlopen("libdl.so", RTLD_NOW | RTLD_NOLOAD);
    if (!libdl) {
        libdl = dlopen("libdl.so", RTLD_NOW);
    }

    if (!libdl) {
        LOGE("Cannot open libdl.so: %s", dlerror());
        return;
    }

    original_dlopen = (dlopen_fn) dlsym(libdl, "dlopen");
    if (!original_dlopen) {
        LOGE("Cannot find dlopen: %s", dlerror());
        dlclose(libdl);
        return;
    }

    LOGI("dlopen found at %p", (void*) original_dlopen);

    // Backup 16 bytes asli
    memcpy(dlopen_backup, (void*) original_dlopen, 16);
    backup_valid = true;

    // Install hook
    if (inline_hook((void*) original_dlopen, (void*) my_dlopen)) {
        LOGI("Pairip bypass installed successfully!");
    } else {
        LOGE("Failed to install Pairip bypass!");
        backup_valid = false;
    }

    dlclose(libdl);
}

// ── JNI entry point ───────────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_geminx_mod_NativeBypass_install(JNIEnv* env, jclass clazz) {
    install_pairip_bypass();
}

// ── Auto-install via constructor ──────────────────────────────────────────────
// Ini dipanggil otomatis saat .so di-load, sebelum JNI_OnLoad
__attribute__((constructor))
static void on_load() {
    LOGI("geminx_native loaded — installing bypass");
    install_pairip_bypass();
}
