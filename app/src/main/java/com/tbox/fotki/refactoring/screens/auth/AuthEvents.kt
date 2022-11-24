package com.tbox.fotki.refactoring.screens.auth

sealed class AuthEvents
object OnSuccess : AuthEvents()
class OnSuccessMsg(val message:String) : AuthEvents()
class OnError(val message: String) : AuthEvents()
class OnFailure(val message: Int) : AuthEvents()
object Loading : AuthEvents()