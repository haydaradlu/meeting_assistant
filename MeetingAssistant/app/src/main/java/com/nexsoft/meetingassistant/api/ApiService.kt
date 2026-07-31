package com.nexsoft.meetingassistant.api

import com.nexsoft.meetingassistant.models.Admin
import com.nexsoft.meetingassistant.models.HasilTranskripsi
import com.nexsoft.meetingassistant.models.Laporan
import com.nexsoft.meetingassistant.models.LoginRequest
import com.nexsoft.meetingassistant.models.LoginResponse
import com.nexsoft.meetingassistant.models.Notulis
import com.nexsoft.meetingassistant.models.PemimpinRapat
import com.nexsoft.meetingassistant.models.RekamanRapat
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    // ==================== Auth ====================

    @POST("auth/login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    // ==================== Admin ====================

    @GET("admin")
    fun getAllAdmins(): Call<List<Admin>>

    @GET("admin/{id}")
    fun getAdminById(@Path("id") id: Int): Call<Admin>

    @POST("admin")
    fun createAdmin(@Body admin: Admin): Call<Admin>

    @PUT("admin/{id}")
    fun updateAdmin(@Path("id") id: Int, @Body admin: Admin): Call<Admin>

    @DELETE("admin/{id}")
    fun deleteAdmin(@Path("id") id: Int): Call<Map<String, String>>

    // ==================== Pemimpin Rapat ====================

    @GET("pemimpin-rapat")
    fun getAllPemimpinRapat(): Call<List<PemimpinRapat>>

    @GET("pemimpin-rapat/{id}")
    fun getPemimpinRapatById(@Path("id") id: Int): Call<PemimpinRapat>

    @POST("pemimpin-rapat")
    fun createPemimpinRapat(@Body pemimpinRapat: PemimpinRapat): Call<PemimpinRapat>

    @PUT("pemimpin-rapat/{id}")
    fun updatePemimpinRapat(@Path("id") id: Int, @Body pemimpinRapat: PemimpinRapat): Call<PemimpinRapat>

    @DELETE("pemimpin-rapat/{id}")
    fun deletePemimpinRapat(@Path("id") id: Int): Call<Map<String, String>>

    // ==================== Notulis ====================

    @GET("notulis")
    fun getAllNotulis(): Call<List<Notulis>>

    @GET("notulis/{id}")
    fun getNotulisById(@Path("id") id: Int): Call<Notulis>

    @POST("notulis")
    fun createNotulis(@Body notulis: Notulis): Call<Notulis>

    @PUT("notulis/{id}")
    fun updateNotulis(@Path("id") id: Int, @Body notulis: Notulis): Call<Notulis>

    @DELETE("notulis/{id}")
    fun deleteNotulis(@Path("id") id: Int): Call<Map<String, String>>

    // ==================== Rekaman Rapat ====================

    @GET("rekaman")
    fun getAllRekaman(): Call<List<RekamanRapat>>

    @Multipart
    @POST("rekaman")
    fun createRekaman(
        @Part("nama_rekaman") namaRekaman: RequestBody,
        @Part fileAudio: MultipartBody.Part,
        @Part("tanggal") tanggal: RequestBody
    ): Call<RekamanRapat>

    @DELETE("rekaman/{id}")
    fun deleteRekaman(@Path("id") id: Int): Call<Map<String, String>>

    // ==================== Hasil Transkripsi ====================

    @GET("hasil")
    fun getAllHasil(): Call<List<HasilTranskripsi>>

    @GET("hasil/{id}")
    fun getHasilById(@Path("id") id: Int): Call<HasilTranskripsi>

    @POST("hasil")
    fun createHasil(@Body hasilTranskripsi: HasilTranskripsi): Call<HasilTranskripsi>

    @PUT("hasil/{id}")
    fun updateHasil(@Path("id") id: Int, @Body hasilTranskripsi: HasilTranskripsi): Call<HasilTranskripsi>

    @DELETE("hasil/{id}")
    fun deleteHasil(@Path("id") id: Int): Call<Map<String, String>>

    @POST("hasil/{id}/transcribe")
    fun transcribeHasil(@Path("id") id: Int): Call<HasilTranskripsi>

    @POST("hasil/{id}/summarize")
    fun summarizeHasil(@Path("id") id: Int): Call<HasilTranskripsi>

    @PUT("hasil/{id}/validate")
    fun validateHasil(@Path("id") id: Int, @Body body: HasilTranskripsi): Call<HasilTranskripsi>

    // ==================== Laporan ====================

    @GET("laporan")
    fun getAllLaporan(): Call<List<Laporan>>

    @GET("laporan/{id}")
    fun getLaporanById(@Path("id") id: Int): Call<Laporan>

    @POST("laporan")
    fun createLaporan(@Body laporan: Laporan): Call<Laporan>

    @DELETE("laporan/{id}")
    fun deleteLaporan(@Path("id") id: Int): Call<Map<String, String>>

    @GET("laporan/{id}/download")
    fun downloadLaporan(@Path("id") id: Int): Call<ResponseBody>
}
