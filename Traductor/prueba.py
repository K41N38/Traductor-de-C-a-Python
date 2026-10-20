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
puntuacion = 100
# Arreglos
edades = [25, 30, 35, 40]
frutas = ["manzana", "naranja", "plátano"]
numeros = [1, 2, 3, 4, 5]
# WHILE principal
contador = 0
while contador < 10:
    if contador % 2 == 0:
        print("Número par: " + contador)
        if contador == 0:
            print("Iniciando conteo")
        elif contador == 2:
            print("Número par medio: " + contador)
            if activo == True:
                print("Sistema activo en caso 2")
                procesados = 0
                while procesados < 3:
                    print("Procesando: " + procesados)
                    procesados += 1
        elif contador == 4:
            print("Número par medio: " + contador)
        elif contador == 6:
            print("Número par medio: " + contador)
            i = 0
            while i < 2:
                print("For interno en caso 6")
                i ++ 
        elif contador == 8:
            print("Número par medio: " + contador)
            # Ordenar arreglo
            numeros.sort()
        else:
            print("Otro número par: " + contador)
    else:
        if contador == 1:
            print("Número impar: " + contador)
            if edad >= 18:
                print("Usuario adulto verificando...")
                if puntuacion == 100:
                    print("¡Puntuación perfecta!")
                    intentos = 2
                    while intentos > 0:
                        print("Intentos: " + intentos)
                        if intentos == 1:
                            print("¡Último intento!")
                        intentos -= 1
                elif puntuacion == 50 or puntuacion == 75:
                    print("Puntuación media")
                    g = 0
                    while g < 2:
                        print("Optimizando...")
                        g ++ 
                else:
                    print("Puntuación: " + puntuacion)
            else:
                print("Usuario menor de edad")
                if not (edad >= 16):
                    print("Acceso restringido")
        elif contador == 3:
            print("Número impar: " + contador)
            for num in numeros:
                if num > 2:
                    print("Número > 2: " + num)
                    if num == 3:
                        calculo = (num * 10) + (edad / 5)
                        print("Cálculo 3: " + calculo)
                    elif num == 4:
                        print("Cuadrado perfecto")
                    elif num == 5:
                        multi = 1
                        while multi <= 2:
                            print(num + " x " + multi + " = " + (num * multi))
                            multi += 1
        elif contador == 5:
            print("Número impar: " + contador)
            repeticiones = 0
            while True:
                print("Do-while ejecución: " + repeticiones + " en contador: " + contador)
                repeticiones += 1
                if not (repeticiones < 3):
                    break
        elif contador == 7:
            print("Número impar: " + contador)
        elif contador == 9:
            print("Número impar: " + contador)
    contador += 1
# Finalización del programa
print("Finalizando programa...")
if puntuacion > 50:
    print("Resultado satisfactorio")
    f = 0
    while f < 2:
        print(f)
        f ++ 
print("=== RESUMEN FINAL ===")
if puntuacion == 0:
    print("Puntuación reiniciada: " + puntuacion)
elif puntuacion == 50 or puntuacion == 75:
    print("Puntuación media final: " + puntuacion)
elif puntuacion == 100:
    print("¡Puntuación perfecta final!")
else:
    print("Nombre: " + nombre + " " + apellido)
    print("Edad: " + edad)
    print("Puntuación: " + puntuacion)
    print("Ciudad: " + ciudad)
print("Programa ejecutado exitosamente")
