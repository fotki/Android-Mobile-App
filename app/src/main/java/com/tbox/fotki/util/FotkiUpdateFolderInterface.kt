package com.tbox.fotki.util

import com.tbox.fotki.model.entities.Folder

/**
* Created by Junaid on 4/25/17.
*/

interface FotkiUpdateFolderInterface {
    fun sendSuccess(folder: Folder)
    fun sendSuccess(name: String, descrip: String)

}
