cmake_minimum_required(VERSION 3.22.1)
project("geminx_native")

# Dobby - hooking library
# Kita pakai dobby sebagai header-only style dengan implementasi manual
# karena tidak bisa download dependency di GitHub Actions tanpa setup khusus
# Sebagai gantinya kita pakai inline PLT hook yang lebih sederhana

add_library(
    geminx_native
    SHARED
    pairip_bypass.cpp
)

target_link_libraries(
    geminx_native
    android
    log
    dl
)
