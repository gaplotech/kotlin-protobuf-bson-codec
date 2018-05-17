package io.github.gaplotech.examples

import io.github.gaplotech.pb.Test

val helloWorld = Test.HelloWorld.newBuilder().apply {
    id = "Asdf123"
    hello = "world"
    world = 321
}.build()
