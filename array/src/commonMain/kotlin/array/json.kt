package array.json

import array.APLValue
import array.ByteProvider

expect fun parseJsonToAPL(input: ByteProvider): APLValue
