package com.tuapp.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

data class LoginRequest(val email: String, val password: String)
data class PedidoRequest(val user: Int, val medicinas: List<Int>, val costototal: Double)

// ✅ Nuevo payload fuerte para UPDATE (evita wildcards)
data class UpdatePedidoPayload(
    val medicinas: List<Int>,
    val costototal: Double,
    val status: String = "P" // opcional, pero muchos backends lo exigen
)

interface ApiService {

    @POST("register/")
    fun register(@Body data: Map<String, Any>): Call<Void>

    @POST("api-token-auth")
    fun login(@Body loginRequest: LoginRequest): Call<Map<String, Any>>

    @GET("users/")
    fun getUserByEmail(@Query("email") email: String): Call<List<Map<String, Any>>>

    @GET("usuariosplanes/get_profile_and_plan_to_app/")
    fun getProfileAndPlanByName(@Query("name") name: String): Call<Map<String, Any>>

    @GET("medicinas/by_category_name/")
    fun getMedicinasByCategoria(@Query("name") nombreCategoria: String): Call<List<Map<String, Any>>>

    @GET("categorias/")
    fun getCategorias(): Call<List<Map<String, Any>>>

    @GET("asignacionmensual/get_asignacion_mensual/")
    fun getAsignacionMensual(@Query("id") id: Int): Call<Map<String, Any>>

    // ——— Pedidos ———
    @POST("pedidos/")
    suspend fun crearPedido(@Body request: PedidoRequest): Response<Map<String, Any>>

    // ✅ Usa el data class (sin wildcards)
    @PATCH("pedidos/{id}/")
    suspend fun updatePedido(
        @Path("id") id: Int,
        @Body payload: UpdatePedidoPayload
    ): Response<Map<String, Any>>

    @POST("pedidos/{id}/finalizar/")
    suspend fun finalizarPedido(@Path("id") id: Int): Response<Unit>

    @GET("pedidos/historial/")
    fun getHistorial(@Query("user_id") userId: Int): Call<List<Map<String, Any>>>

    // Dos variantes del “en progreso”
    @GET("pedidos/en-progreso/{user}/")
    suspend fun getPedidoEnProgreso(@Path("user") userId: Int): Response<Map<String, Any>>

    @GET("pedidos/en_progreso/")
    suspend fun getPedidoEnProgresoAlt(@Query("user_id") userId: Int): Response<Map<String, Any>>
}
