package array.json

import array.APLValue
import array.CharacterProvider

expect fun parseJsonToAPL(input: CharacterProvider): APLValue
