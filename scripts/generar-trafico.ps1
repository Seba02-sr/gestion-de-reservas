<#
.SYNOPSIS
  Genera trafico por el API Gateway para ver datos en Grafana (metricas, logs y trazas).

.DESCRIPTION
  Hace varios pedidos GET, da de alta datos de ejemplo (hotel, tarifa, huesped, habitacion -> evento
  RabbitMQ gestion-svc -> reservas-svc) y crea reservas (201), una con huesped inexistente (404) y
  una superpuesta (409). Tambien intenta apagar user-svc por el gateway (debe dar 404).

  Uso:   .\scripts\generar-trafico.ps1
         .\scripts\generar-trafico.ps1 -Rondas 50 -Gateway http://localhost:8080
#>
param(
    [string]$Gateway = "http://localhost:8080",
    [int]$Rondas = 20
)

$ErrorActionPreference = "Stop"
$resumen = New-Object System.Collections.Generic.List[object]

function Invoke-Api {
    param([string]$Metodo, [string]$Ruta, $Cuerpo = $null, [string]$Etiqueta = "")
    $params = @{ Uri = "$Gateway$Ruta"; Method = $Metodo; UseBasicParsing = $true; ErrorAction = "Stop" }
    if ($null -ne $Cuerpo) {
        $params.Body = ($Cuerpo | ConvertTo-Json -Depth 6)
        $params.ContentType = "application/json; charset=utf-8"
    }
    $codigo = 0; $texto = ""
    try {
        $r = Invoke-WebRequest @params
        $codigo = [int]$r.StatusCode; $texto = $r.Content
    } catch {
        if ($_.Exception.Response) {
            $codigo = [int]$_.Exception.Response.StatusCode
            try {
                $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $texto = $sr.ReadToEnd()
            } catch { $texto = "" }
        } else { $codigo = -1; $texto = $_.Exception.Message }
    }
    if ($Etiqueta) { $resumen.Add([pscustomobject]@{ Pedido = $Etiqueta; Estado = $codigo }) }
    $json = $null
    try { if ($texto) { $json = $texto | ConvertFrom-Json } } catch { }
    return [pscustomobject]@{ Codigo = $codigo; Json = $json; Texto = $texto }
}

# CUIT con digito verificador valido (el hotel lo exige)
function New-Cuit {
    $pesos = 5, 4, 3, 2, 7, 6, 5, 4, 3, 2
    while ($true) {
        $base = "30" + (Get-Random -Minimum 10000000 -Maximum 99999999)
        $suma = 0
        for ($i = 0; $i -lt 10; $i++) { $suma += [int]::Parse($base[$i]) * $pesos[$i] }
        $dv = 11 - ($suma % 11)
        if ($dv -eq 11) { $dv = 0 }
        if ($dv -ne 10) { return "$base$dv" }   # dv = 10 no existe: se sortea otro numero base
    }
}

Write-Host "== Trafico de lectura ($Rondas rondas) =="
for ($i = 1; $i -le $Rondas; $i++) {
    Invoke-Api GET "/gestion/hoteles" -Etiqueta "GET /gestion/hoteles" | Out-Null
    Invoke-Api GET "/users/huespedes" -Etiqueta "GET /users/huespedes" | Out-Null
    Invoke-Api GET "/reservas/habitaciones/disponibles?lat=-31.6333&lng=-60.7&radioKm=50" -Etiqueta "GET /reservas/habitaciones/disponibles" | Out-Null
    Invoke-Api GET "/gestion/info" -Etiqueta "GET /gestion/info" | Out-Null
    Invoke-Api GET "/users/info" -Etiqueta "GET /users/info" | Out-Null
    Invoke-Api GET "/reservas/info" -Etiqueta "GET /reservas/info" | Out-Null
}

Write-Host "== Datos de ejemplo =="
$sufijo = Get-Random -Minimum 1000 -Maximum 9999

$hotel = Invoke-Api POST "/gestion/hoteles/registrar" @{
    nombre = "Hotel Trafico $sufijo"; cuit = (New-Cuit); domicilio = "Bv. Pellegrini 2500, Santa Fe"
    latitud = -31.6333; longitud = -60.7; telefono = "342-4555000"; correoContacto = "trafico$sufijo@example.com"; categoria = 4
} "POST /gestion/hoteles/registrar"
if ($hotel.Codigo -ne 201) { throw "No se pudo crear el hotel: $($hotel.Codigo) $($hotel.Texto)" }
$idHotel = $hotel.Json.id

Invoke-Api POST "/gestion/tarifas" @{ nombre = "Tarifa trafico $sufijo"; idTipoHabitacion = 3; precioNoche = 15000 } "POST /gestion/tarifas" | Out-Null

$bancos = Invoke-Api GET "/users/bancos" -Etiqueta "GET /users/bancos"
$idBanco = $null
if ($bancos.Json -and @($bancos.Json).Count -gt 0) { $idBanco = @($bancos.Json)[0].id }
else {
    $b = Invoke-Api POST "/users/bancos" @{ nombre = "Banco Trafico" } "POST /users/bancos"
    $idBanco = $b.Json.id
}

$dni = [string](Get-Random -Minimum 20000000 -Maximum 49999999)
$huesped = Invoke-Api POST "/users/users/huesped" @{
    nombre = "Huesped Trafico $sufijo"; email = "huesped$sufijo@example.com"; telefono = "+54 9 342-555-1234"
    dni = $dni; username = "trafico$sufijo"; password = "secret"; fechaNacimiento = "1990-01-01"
    tarjetasCredito = @(@{ numero = "5500000000000004"; nombreTitular = "Huesped Trafico"; fechaVencimiento = "12/2027"; cvc = "123"; esPrincipal = $true; idBanco = $idBanco })
} "POST /users/users/huesped"
if ($huesped.Codigo -ne 201) { throw "No se pudo crear el huesped: $($huesped.Codigo) $($huesped.Texto)" }
# el alta responde 201 con el huesped creado (HuespedResponse) y el header Location
$idHuesped = $huesped.Json.id
if (-not $idHuesped) { throw "El alta de huesped no devolvio el id: $($huesped.Texto)" }

# Crear habitacion: gestion-svc guarda y PUBLICA un evento en RabbitMQ; reservas-svc lo consume.
# En Tempo es UNA traza que continua en reservas-svc.
$numero = Get-Random -Minimum 100 -Maximum 999
$hab = Invoke-Api POST "/gestion/habitaciones" @{ numero = $numero; piso = 1; idTipoHabitacion = 3; idHotel = $idHotel; disponible = $true } "POST /gestion/habitaciones (evento RabbitMQ)"
if ($hab.Codigo -ne 201) { throw "No se pudo crear la habitacion: $($hab.Codigo) $($hab.Texto)" }
$idHab = $hab.Json.id

Write-Host "   hotel=$idHotel huesped=$idHuesped habitacion=$idHab (esperando la replicacion por RabbitMQ...)"
$replicada = $false
for ($i = 0; $i -lt 20 -and -not $replicada; $i++) {
    Start-Sleep -Milliseconds 500
    $replicada = (Invoke-Api GET "/reservas/habitaciones/$idHab").Codigo -eq 200
}
if (-not $replicada) { throw "La habitacion $idHab no se replico en reservas-svc" }

Write-Host "== Reservas =="
$desde = (Get-Date).AddDays(30 + (Get-Random -Minimum 0 -Maximum 300))
$ci = $desde.ToString("yyyy-MM-dd"); $co = $desde.AddDays(5).ToString("yyyy-MM-dd")
$ok = Invoke-Api POST "/reservas/reservas" @{ habitacionId = $idHab; idHuesped = $idHuesped; checkIn = $ci; checkOut = $co } "POST /reservas/reservas (huesped existente) -> 201"
Invoke-Api POST "/reservas/reservas" @{ habitacionId = $idHab; idHuesped = $idHuesped; checkIn = $ci; checkOut = $co } "POST /reservas/reservas (fechas ocupadas) -> 409" | Out-Null
Invoke-Api POST "/reservas/reservas" @{ habitacionId = $idHab; idHuesped = 999999; checkIn = $ci; checkOut = $co } "POST /reservas/reservas (huesped inexistente) -> 404" | Out-Null
if ($ok.Codigo -eq 201) {
    Invoke-Api POST "/reservas/reservas/$($ok.Json.id)/pagos" @{ method = "TARJETA"; monto = 10000 } "POST pago -> CONFIRMADA" | Out-Null
}

Write-Host "== Seguridad: el actuator no sale por el gateway =="
Invoke-Api POST "/users/actuator/shutdown" -Etiqueta "POST /users/actuator/shutdown -> 404" | Out-Null
Invoke-Api GET "/gestion/actuator/prometheus" -Etiqueta "GET /gestion/actuator/prometheus -> 404" | Out-Null

Write-Host ""
Write-Host "== Resumen =="
$resumen | Group-Object Pedido, Estado | ForEach-Object {
    [pscustomobject]@{ Pedido = $_.Group[0].Pedido; Estado = $_.Group[0].Estado; Cantidad = $_.Count }
} | Format-Table -AutoSize | Out-Host

if ($ok.Codigo -eq 201) {
    Write-Host "Reserva creada: $($ok.Json.id)  (habitacion $idHab, huesped $idHuesped)"
    Write-Host "Ahora abri Grafana (http://localhost:3000): Explore -> Tempo -> buscar service.name=reservas-svc"
}
