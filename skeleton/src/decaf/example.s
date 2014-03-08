foo:

enter $(8*2), $0

mov %rdi, -8(%rbp)

mov -8(%rbp), %r10

add $3, %r10

mov %r10, -16(%rbp)

mov -16(%rbp), %rax

leave

ret 

.globl main

main:

enter $(8 * 6), $0

call get_int_035

mov %rax, -8(%rbp)

mov -8(%rbp), %rdi

call foo

mov %rax, -16(%rbp)

mov -16(%rbp), %r10

mov %r10, -24(%rbp)

mov -24(%rbp), %r10

mov $15, %r11

cmp %r10, %r11

mov $0, %r11

mov $1, %r10

cmove %r10, %r11

mov %r11, -32(%rbp)

mov -32(%rbp), %r10

mov $1, %r11

cmp %r10, %r11

je .fifteen

mov $.what, %rdi

mov -24(%rbp), %rsi

mov $0, %rax

call printf

mov %rax, -40(%rbp)

jmp .fifteen_done

.fifteen:

mov $.indeed, %rdi

mov $0, %rax

call printf

mov %rax, -48(%rbp)

.fifteen_done:

mov $0, %rax

leave

ret

.indeed:

.string "Indeed, \'tis 15!\n"

.what:

.string "What! %d\n"
