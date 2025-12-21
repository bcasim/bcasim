f = open('network.csv', 'w')

for i in range(0,100):
    for j in range(0,100):
        if i==j:
            f.write('0,')
        elif j == 99:
            f.write('1')
            f.write('\n')
        else:
            f.write('1,')




f.close()