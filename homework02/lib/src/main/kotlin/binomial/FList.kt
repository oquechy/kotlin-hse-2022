package binomial

import binomial.FList.Companion.nil
import java.lang.Thread.yield

/*
 * FList - реализация функционального списка
 *
 * Пустому списку соответствует тип Nil, непустому - Cons
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 *  Исключение Array-параметр в функции flistOf. Но даже в ней нельзя использовать цикл и forEach.
 *  Только обращение по индексу
 */
private typealias ListProducer<T> = (Unit) -> FList<T>

sealed class FList<T>: Iterable<T> {
    // размер списка, 0 для Nil, количество элементов в цепочке для Cons
    abstract val size: Int

    // пустой ли списк, true для Nil, false для Cons
    abstract val isEmpty: Boolean

    // получить список, применив преобразование
    // требуемая сложность - O(n)
    abstract fun <U> map(f: (T) -> U): FList<U>

    // получить список из элементов, для которых f возвращает true
    // требуемая сложность - O(n)
    abstract fun filter(f: (T) -> Boolean): FList<T>

    // свертка
    // требуемая сложность - O(n)
    // Для каждого элемента списка (curr) вызываем f(acc, curr),
    // где acc - это base для начального элемента, или результат вызова
    // f(acc, curr) для предыдущего
    // Результатом fold является результат последнего вызова f(acc, curr)
    // или base, если список пуст
    abstract fun <U> fold(base: U, f: (U, T) -> U): U

    // разворот списка
    // требуемая сложность - O(n)
    fun reverse(): FList<T> {
        fun reverseRec(l : FList<T>, acc : FList<T>) : FList<T> {
            return when (l) {
                is Cons -> reverseRec(l.tail, Cons(l.head, acc))
                is Nil -> acc
            }
        }
        return reverseRec(this, nil())
    }

    /*
     * Это не очень красиво, что мы заводим отдельный Nil на каждый тип
     * И вообще лучше, чтобы Nil был объектом
     *
     * Но для этого нужны приседания с ковариантностью
     *
     * dummy - костыль для того, что бы все Nil-значения были равны
     *         и чтобы Kotlin-компилятор был счастлив (он требует, чтобы у Data-классов
     *         были свойство)
     *
     * Также для борьбы с бойлерплейтом были введены функция и свойство nil в компаньоне
     */
    data class Nil<T>(private val dummy: Int = 0) : FList<T>() {
        override val size: Int = 0

        override val isEmpty: Boolean = true

        override fun <U> map(f: (T) -> U): FList<U> = nil()


        override fun filter(f: (T) -> Boolean): FList<T> = nil()

        override fun <U> fold(base: U, f: (U, T) -> U): U = base

        override fun iterator(): Iterator<T> = object : Iterator<T> {
            override fun hasNext(): Boolean = false

            override fun next(): T = throw NoSuchElementException()
        }
    }

    data class Cons<T>(val head: T, val tail: FList<T>) : FList<T>() {
        override val size: Int
            get() = 1 + tail.size

        override val isEmpty: Boolean = false

        override fun <U> map(f: (T) -> U): FList<U> = Cons(f(head), tail.map(f))

        override fun filter(f: (T) -> Boolean): FList<T> =
            if (f(head)) {
                Cons(head, tail.filter(f))
            } else {
                tail.filter(f)
            }

        override fun <U> fold(base: U, f: (U, T) -> U): U = f(tail.fold(base, f), head)

        override fun iterator(): Iterator<T> = object : Iterator<T> {
            // Не получилось без var :(
            var cur :FList<T> = this@Cons

            override fun hasNext(): Boolean = when (cur) {
                is Cons -> true
                is Nil -> false
            }

            override fun next(): T {
                return when (cur) {
                    is Cons -> {
                        val ret = (cur as Cons<T>).head
                        cur = (cur as Cons<T>).tail
                        ret
                    }
                    is Nil -> throw NoSuchElementException()
                }
            }
        }
    }

    companion object {
        fun <T> nil() = Nil<T>()
        val nil = Nil<Any>()
    }
}

// конструирование функционального списка в порядке следования элементов
// требуемая сложность - O(n)
fun <T> flistOf(vararg values: T): FList<T> {
    fun flistOfRec(i: Int, values: Array<out T>): FList<T> =
        if (i >= values.size) {
            nil()
        } else {
            FList.Cons(values[i], flistOfRec(i + 1, values))
        }

    return flistOfRec(0, values)
}

