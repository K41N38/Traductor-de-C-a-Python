i = 0
while i < 2:
    j = 0
    while j < 3:
        j += 1
        if j == 2:
            continue
        print(j)
    print(i)
    i += 1
