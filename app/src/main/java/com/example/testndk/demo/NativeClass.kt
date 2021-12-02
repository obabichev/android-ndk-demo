package com.example.testndk.demo

class NativeClass {
    companion object {
        external fun testFunction(addrRgba: Long)
    }
}