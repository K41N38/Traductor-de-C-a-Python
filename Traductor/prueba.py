# Variables
edad = 25
matematica = 85
ciencia = 90
historia = 78
x = 5
y = 10
z = 15
nombre = "Carlos"
apellido = "López"
ciudad = "Madrid"
pais = "España"
activo = True
puntuacion = 100
# Arreglos
edades = [25, 30, 35, 40]
frutas = ["manzana", "naranja", "plátano"]
flags = [True, False, True]
numeros = [1, 2, 3, 4, 5]
# WHILE principal
contador = 0
while contador < 10:
    if contador % 2 == 0:
        print("Número par: " + str(contador))
        if contador == 0:
            print("Iniciando conteo")
        elif contador == 2:
            print("Número par medio: " + str(contador))
            if activo == True:
                print("Sistema activo en caso 2")
                procesados = 0
                while procesados < 3:
                    print("Procesando: " + str(procesados))
                    procesados += 1
        elif contador == 4:
            print("Número par medio: " + str(contador))
            try:
                calculo = int(contador / (contador - 2))
                print("Cálculo exitoso: " + str(calculo))
            except ArithmeticError:
                print("Error en cálculo para: " + str(contador))
        elif contador == 6:
            print("Número par medio: " + str(contador))
            i = 0
            while i < 2:
                print("For interno en caso 6")
                i += 1
        elif contador == 8:
            print("Número par medio: " + str(contador))
            # Longitud del texto
            print(len(("texto prueba")))
            # Ordenar arreglo
            numeros.sort()
        else:
            print("Otro número par: " + str(contador))
    else:
        if contador == 1:
            print("Número impar: " + str(contador))
            if edad >= 18:
                print("Usuario adulto verificando...")
                if puntuacion == 100:
                    print("¡Puntuación perfecta!")
                    intentos = 2
                    while intentos > 0:
                        print("Intentos: " + str(intentos))
                        if intentos == 1:
                            print("¡Último intento!")
                        intentos -= 1
                elif puntuacion == 50 or puntuacion == 75:
                    print("Puntuación media")
                    j = 0
                    while j < 2:
                        print("Optimizando...")
                        j += 1
                else:
                    print("Puntuación: " + str(puntuacion))
                    try:
                        datos = ["a", "b"]
                        for item in datos:
                            print("Dato: " + str(item))
                    except Exception as error:
                        print("Error datos: " + str((error.args[0] if error.args else "")))
            else:
                print("Usuario menor de edad")
                if not (edad >= 16):
                    print("Acceso restringido")
        elif contador == 3:
            print("Número impar: " + str(contador))
            for num in numeros:
                if num > 2:
                    print("Número > 2: " + str(num))
                    if num == 3:
                        calculoo = (num * 10) + (int(edad / 5))
                        print("Cálculo 3: " + str(calculoo))
                    elif num == 4:
                        print("Cuadrado perfecto")
                    elif num == 5:
                        multi = 1
                        while multi <= 2:
                            print(str(num) + " x " + str(multi) + " = " + str((num * multi)))
                            multi += 1
        elif contador == 5:
            print("Número impar: " + str(contador))
            repeticiones = 0
            while True:
                print("Do-while ejecución: " + str(repeticiones) + " en contador: " + str(contador))
                repeticiones += 1
                if not (repeticiones < 3):
                    break
        elif contador == 7:
            print("Número impar: " + str(contador))
        elif contador == 9:
            print("Número impar: " + str(contador))
            try:
                print("Proceso crítico final...")
                if activo:
                    try:
                        division = int(100 / (edad - 25))
                        print("División: " + str(division))
                    except ArithmeticError:
                        print("División fallida")
                if puntuacion < 0:
                    raise InvalidOperationException("Puntuación inválida")
            except Exception as errorCritico:
                print("Error: " + str((errorCritico.args[0] if errorCritico.args else "")))
                if (errorCritico.args[0] if errorCritico.args else "") == "Puntuación inválida":
                    puntuacion = 0
                    print("Puntuación reiniciada")
                else:
                    print("Error no manejado")
            finally:
                print("Proceso finalizado")
    contador += 1
# Finalización del programa
print("Finalizando programa...")
if puntuacion > 50:
    print("Resultado satisfactorio")
    k = 0
    while k < 2:
        try:
            finalScore = puntuacion * 2
            print("Puntuación final: " + str(finalScore))
            if not (edad >= 18 and activo):
                print("Acceso denegado final")
        except Exception:
            print("Error cálculo final")
        k += 1
print("=== RESUMEN FINAL ===")
if puntuacion == 0:
    print("Puntuación reiniciada: " + str(puntuacion))
elif puntuacion == 50 or puntuacion == 75:
    print("Puntuación media final: " + str(puntuacion))
elif puntuacion == 100:
    print("¡Puntuación perfecta final!")
else:
    print("Nombre: " + nombre + " " + apellido)
    print("Edad: " + str(edad))
    print("Puntuación: " + str(puntuacion))
    print("Estado: " + str(activo))
    print("Ciudad: " + ciudad)
print("Programa ejecutado exitosamente")
