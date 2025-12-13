package com.tuapp.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

data class LoginRequest(val email: String, val password: String)
data class PedidoRequest(val user: Int, val medicinas: List<Int>, val costototal: Double)

// Payload fuerte (evita wildcards en Retrofit)
data class UpdatePedidoPayload(
    val medicinas: List<Int>? = null,
    val costototal: Double? = null,
    val status: String? = null // "P" o "C"
)

// ✅ MODELOS del endpoint get_user_information
data class UserInformationResponse(
    val user_id: Int? = null,              // recomendado que backend lo mande
    val user_name: String = "",
    val shipping_address: String? = null,
    val user_monthly_budget: Double? = null,
    val health_plan: HealthPlanDto? = null
)

data class HealthPlanDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val monthly_budget: Double? = null,
    val health_insurance: InsuranceDto? = null
)

data class InsuranceDto(
    val name: String = "",
    val description: String? = null,
    val email: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val insurance_id_number: String? = null
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

    // —— Pedidos ——
    @POST("pedidos/")
    suspend fun crearPedido(@Body request: PedidoRequest): Response<Map<String, Any>>

    @PATCH("pedidos/{id}/")
    suspend fun updatePedido(
        @Path("id") id: Int,
        @Body payload: UpdatePedidoPayload
    ): Response<Map<String, Any>>

    @GET("pedidos/historial/")
    fun getHistorial(@Query("user_id") userId: Int): Call<List<Map<String, Any>>>

    // ÚNICO endpoint de "en progreso" en tu backend
    @GET("pedidos/en_progreso/")
    suspend fun getPedidoEnProgreso(@Query("user_id") userId: Int): Response<Map<String, Any>>

    @GET("asignacionmensual/get_asignaciones_por_plan_salud/")
    suspend fun getAsignacionesPorPlan(
        @Query("plan_salud") planSaludId: Int
    ): Response<List<Map<String, Any>>>

    // ✅ OJO: SIN "/" inicial (evita romper baseUrl)
    @GET("usuariosplanes/get_user_information/")
    fun getUserInformation(
        @Query("user_id") userIdOrEmailOrName: String
    ): Call<UserInformationResponse>
}
