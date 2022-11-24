package com.pro.produktydb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tbox.fotki.model.database.FilesEntity

@Dao
interface HistoryDao {
  @get:Query("SELECT * FROM filesentity ORDER BY id DESC")
  val all: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=0 ORDER BY id DESC")
  val allNotLoaded: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=3 ORDER BY id DESC")
  val allForSync: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=3 ORDER BY id DESC LIMIT 70")
  val next70ForSync: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=3 ORDER BY id DESC LIMIT 30")
  val next30ForSync: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=3 ORDER BY id DESC LIMIT 10")
  val next10ForSync: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=0 OR loadedStatus=3 ORDER BY id DESC")
  val allInProgress: List<FilesEntity>

  @get:Query("SELECT * FROM filesentity WHERE loadedStatus=4 ORDER BY id DESC")
  val allNotChecked: List<FilesEntity>


  @Query("SELECT * FROM filesentity WHERE fileName=:fileName ORDER BY id DESC")
  fun allByFileName(fileName: String) :List<FilesEntity>

  @Insert
  fun insertAll(vararg users: FilesEntity)

  @Insert
  fun insert(user: FilesEntity): Long

  @Update
  fun update(file:FilesEntity):Int

  @Query("UPDATE filesentity SET loadedStatus=:status WHERE fileName = :fileName")
  fun updateNewStatus(fileName: String, status:Int)

  @Query("UPDATE filesentity SET loadedStatus=:status WHERE hashSHA = :sha")
  fun updateNewStatusSHA(sha: String, status:Int)

  @Query("DELETE FROM filesentity")
  fun deleteAll()

  @Query("DELETE FROM filesentity WHERE id = :id")
  fun deleteById(id: Int)

  @Query("DELETE FROM filesentity WHERE fileName = :fileName")
  fun deleteByBarcode(fileName: String)
}