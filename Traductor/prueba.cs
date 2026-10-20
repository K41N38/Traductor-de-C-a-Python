        // Variables
        int edad = 25, matematica = 85, ciencia = 90,
            historia = 78, x = 5, y = 10, z = 15;

        string nombre = "Carlos", apellido = "López",
            ciudad = "Madrid", pais = "España";
        int puntuacion = 100;

        // Arreglos
        int[] edades = { 25, 30, 35, 40 };
        string[] frutas = { "manzana", "naranja", "plátano" };
        int[] numeros = { 1, 2, 3, 4, 5 };

        // WHILE principal
        int contador = 0;

        while (contador < 10)
        {
            if (contador % 2 == 0)
            {
                Console.WriteLine("Número par: " + contador);

                switch (contador)
                {
                    case 0:
                    {
                        Console.WriteLine("Iniciando conteo");
                        break;
                    }

                    case 2:
                    {
                        Console.WriteLine("Número par medio: " + contador);

                        if (activo == true)
                        {
                            Console.WriteLine("Sistema activo en caso 2");

                            int procesados = 0;

                            while (procesados < 3)
                            {
                                Console.WriteLine("Procesando: " + procesados);
                                procesados += 1;
                            }
                        }

                        break;
                    }

                    case 4:
                    {
                        Console.WriteLine("Número par medio: " + contador);

                        break;
                    }

                    case 6:
                    {
                        Console.WriteLine("Número par medio: " + contador);

                        for (int i = 0; i < 2; i++)
                        {
                            Console.WriteLine("For interno en caso 6");
                        }

                        break;
                    }

                    case 8:
                    {
                        Console.WriteLine("Número par medio: " + contador);

                        // Ordenar arreglo
                        Array.Sort(numeros);
                        break;
                    }

                    default:
                        Console.WriteLine("Otro número par: " + contador);
                        break;
                }
            }
            else
            {
                switch (contador)
                {
                    case 1:
                    {
                        Console.WriteLine("Número impar: " + contador);

                        if (edad >= 18)
                        {
                            Console.WriteLine("Usuario adulto verificando...");

                            switch (puntuacion)
                            {
                                case 100:
                                {
                                    Console.WriteLine("¡Puntuación perfecta!");

                                    int intentos = 2;

                                    while (intentos > 0)
                                    {
                                        Console.WriteLine("Intentos: " + intentos);

                                        if (intentos == 1)
                                        {
                                            Console.WriteLine("¡Último intento!");
                                        }

                                        intentos -= 1;
                                    }

                                    break;
                                }

                                case 50:
                                case 75:
                                {
                                    Console.WriteLine("Puntuación media");

                                    for (int g = 0; g < 2; g++)
                                    {
                                        Console.WriteLine("Optimizando...");
                                    }

                                    break;
                                }

                                default:
                                {
                                    Console.WriteLine("Puntuación: " + puntuacion);

                                    break;
                                }
                            }
                        }
                        else
                        {
                            Console.WriteLine("Usuario menor de edad");

                            if (!(edad >= 16))
                            {
                                Console.WriteLine("Acceso restringido");
                            }
                        }

                        break;
                    }

                    case 3:
                    {
                        Console.WriteLine("Número impar: " + contador);

                        foreach (int num in numeros)
                        {
                            if (num > 2)
                            {
                                Console.WriteLine("Número > 2: " + num);

                                switch (num)
                                {
                                    case 3:
                                    {
                                        int calculo = (num * 10) + (edad / 5);
                                        Console.WriteLine("Cálculo 3: " + calculo);
                                        break;
                                    }

                                    case 4:
                                    {
                                        Console.WriteLine("Cuadrado perfecto");

                                        break;
                                    }

                                    case 5:
                                    {
                                        int multi = 1;

                                        while (multi <= 2)
                                        {
                                            Console.WriteLine(
                                                num + " x " + multi +
                                                " = " + (num * multi));

                                            multi += 1;
                                        }

                                        break;
                                    }
                                }
                            }
                        }

                        break;
                    }

                    case 5:
                    {
                        Console.WriteLine("Número impar: " + contador);

                        int repeticiones = 0;

                        do
                        {
                            Console.WriteLine(
                                "Do-while ejecución: " + repeticiones +
                                " en contador: " + contador);

                            repeticiones += 1;
                        }
                        while (repeticiones < 3);

                        break;
                    }

                    case 7:
                    {
                        Console.WriteLine("Número impar: " + contador);

                        break;
                    }

                    case 9:
                    {
                        Console.WriteLine("Número impar: " + contador);

                        break;
                    }
                }
            }

            contador += 1;
        }

        // Finalización del programa
        Console.WriteLine("Finalizando programa...");

        if (puntuacion > 50)
        {
            Console.WriteLine("Resultado satisfactorio");

            for (int f = 0; f < 2; f++)
            {
                Console.WriteLine(f);
            }
        }

        Console.WriteLine("=== RESUMEN FINAL ===");

        switch (puntuacion)
        {
            case 0:
                Console.WriteLine("Puntuación reiniciada: " + puntuacion);
                break;

            case 50:
            case 75:
                Console.WriteLine("Puntuación media final: " + puntuacion);
                break;

            case 100:
                Console.WriteLine("¡Puntuación perfecta final!");
                break;

            default:
                Console.WriteLine("Nombre: " + nombre + " " + apellido);
                Console.WriteLine("Edad: " + edad);
                Console.WriteLine("Puntuación: " + puntuacion);
                Console.WriteLine("Ciudad: " + ciudad);
                break;
        }

        Console.WriteLine("Programa ejecutado exitosamente");