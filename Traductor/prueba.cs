for (int i = 0; i < 2; i++) {
    int j = 0;

    while (j < 3) {
        j++;

        if (j == 2) {
            continue;
        }

        Console.WriteLine(j);
    }

    Console.WriteLine(i);
}