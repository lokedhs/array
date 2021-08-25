package array

import array.complex.*
import kotlin.jvm.Strictfp
import kotlin.math.*

private data class DoubleSet(val a: Double, val b: Double)

private const val LEFT = -.3955078125
private const val x0 = .461632144968362356785

private const val a0_hi = 0.88560319441088874992
private const val a0_lo = -.00000000000000004996427036469019695
private const val P0 = 6.21389571821820863029017800727e-01
private const val P1 = 2.65757198651533466104979197553e-01
private const val P2 = 5.53859446429917461063308081748e-03
private const val P3 = 1.38456698304096573887145282811e-03
private const val P4 = 2.40659950032711365819348969808e-03
private const val Q0 = 1.45019531250000000000000000000e+00
private const val Q1 = 1.06258521948016171343454061571e+00
private const val Q2 = -2.07474561943859936441469926649e-01
private const val Q3 = -1.46734131782005422506287573015e-01
private const val Q4 = 3.07878176156175520361557573779e-02
private const val Q5 = 5.12449347980666221336054633184e-03
private const val Q6 = -1.76012741431666995019222898833e-03
private const val Q7 = 9.35021023573788935372153030556e-05
private const val Q8 = 6.13275507472443958924745652239e-06

private const val lns2pi_hi = 0.418945312500000
private const val lns2pi_lo = -.000006779295327258219670263595
private const val Pa0 = 8.33333333333333148296162562474e-02
private const val Pa1 = -2.77777777774548123579378966497e-03
private const val Pa2 = 7.93650778754435631476282786423e-04
private const val Pa3 = -5.95235082566672847950717262222e-04
private const val Pa4 = 8.41428560346653702135821806252e-04
private const val Pa5 = -1.89773526463879200348872089421e-03
private const val Pa6 = 5.69394463439411649408050664078e-03
private const val Pa7 = -1.44705562421428915453880392761e-02

private const val ln2_hi = 6.93147180369123816490e-01
private const val ln2_lo = 1.90821492927058770002e-10
private const val two54 = 1.80143985094819840000e+16
private const val Lg1 = 6.666666666666735130e-01
private const val Lg2 = 3.999999999940941908e-01
private const val Lg3 = 2.857142874366239149e-01
private const val Lg4 = 2.222219843214978396e-01
private const val Lg5 = 1.818357216161805012e-01
private const val Lg6 = 1.531383769920937332e-01
private const val Lg7 = 1.479819860511658591e-01

private const val N = 128

private const val A1 = .08333333333333178827
private const val A2 = .01250000000377174923
private const val A3 = .002232139987919447809
private const val A4 = .0004348877777076145742

private val logF_head = doubleArrayOf(
    0.0,
    .007782140442060381246,
    .015504186535963526694,
    .023167059281547608406,
    .030771658666765233647,
    .038318864302141264488,
    .045809536031242714670,
    .053244514518837604555,
    .060624621816486978786,
    .067950661908525944454,
    .075223421237524235039,
    .082443669210988446138,
    .089612158689760690322,
    .096729626458454731618,
    .103796793681567578460,
    .110814366340264314203,
    .117783035656430001836,
    .124703478501032805070,
    .131576357788617315236,
    .138402322859292326029,
    .145182009844575077295,
    .151916042025732167530,
    .158605030176659056451,
    .165249572895390883786,
    .171850256926518341060,
    .178407657472689606947,
    .184922338493834104156,
    .191394852999565046047,
    .197825743329758552135,
    .204215541428766300668,
    .210564769107350002741,
    .216873938300523150246,
    .223143551314024080056,
    .229374101064877322642,
    .235566071312860003672,
    .241719936886966024758,
    .247836163904594286577,
    .253915209980732470285,
    .259957524436686071567,
    .265963548496984003577,
    .271933715484010463114,
    .277868451003087102435,
    .283768173130738432519,
    .289633292582948342896,
    .295464212893421063199,
    .301261330578199704177,
    .307025035294827830512,
    .312755710004239517729,
    .318453731118097493890,
    .324119468654316733591,
    .329753286372579168528,
    .335355541920762334484,
    .340926586970454081892,
    .346466767346100823488,
    .351976423156884266063,
    .357455888922231679316,
    .362905493689140712376,
    .368325561158599157352,
    .373716409793814818840,
    .379078352934811846353,
    .384411698910298582632,
    .389716751140440464951,
    .394993808240542421117,
    .400243164127459749579,
    .405465108107819105498,
    .410659924985338875558,
    .415827895143593195825,
    .420969294644237379543,
    .426084395310681429691,
    .431173464818130014464,
    .436236766774527495726,
    .441274560805140936281,
    .446287102628048160113,
    .451274644139630254358,
    .456237433481874177232,
    .461175715122408291790,
    .466089729924533457960,
    .470979715219073113985,
    .475845904869856894947,
    .480688529345570714212,
    .485507815781602403149,
    .490303988045525329653,
    .495077266798034543171,
    .499827869556611403822,
    .504556010751912253908,
    .509261901790523552335,
    .513945751101346104405,
    .518607764208354637958,
    .523248143765158602036,
    .527867089620485785417,
    .532464798869114019908,
    .537041465897345915436,
    .541597282432121573947,
    .546132437597407260909,
    .550647117952394182793,
    .555141507540611200965,
    .559615787935399566777,
    .564070138285387656651,
    .568504735352689749561,
    .572919753562018740922,
    .577315365035246941260,
    .581691739635061821900,
    .586049045003164792433,
    .590387446602107957005,
    .594707107746216934174,
    .599008189645246602594,
    .603290851438941899687,
    .607555250224322662688,
    .611801541106615331955,
    .616029877215623855590,
    .620240409751204424537,
    .624433288012369303032,
    .628608659422752680256,
    .632766669570628437213,
    .636907462236194987781,
    .641031179420679109171,
    .645137961373620782978,
    .649227946625615004450,
    .653301272011958644725,
    .657358072709030238911,
    .661398482245203922502,
    .665422632544505177065,
    .669430653942981734871,
    .673422675212350441142,
    .677398823590920073911,
    .681359224807238206267,
    .685304003098281100392,
    .689233281238557538017,
    .693147180560117703862)

private val logF_tail = doubleArrayOf(
    0.0,
    -.00000000000000543229938420049,
    .00000000000000172745674997061,
    -.00000000000001323017818229233,
    -.00000000000001154527628289872,
    -.00000000000000466529469958300,
    .00000000000005148849572685810,
    -.00000000000002532168943117445,
    -.00000000000005213620639136504,
    -.00000000000001819506003016881,
    .00000000000006329065958724544,
    .00000000000008614512936087814,
    -.00000000000007355770219435028,
    .00000000000009638067658552277,
    .00000000000007598636597194141,
    .00000000000002579999128306990,
    -.00000000000004654729747598444,
    -.00000000000007556920687451336,
    .00000000000010195735223708472,
    -.00000000000017319034406422306,
    -.00000000000007718001336828098,
    .00000000000010980754099855238,
    -.00000000000002047235780046195,
    -.00000000000008372091099235912,
    .00000000000014088127937111135,
    .00000000000012869017157588257,
    .00000000000017788850778198106,
    .00000000000006440856150696891,
    .00000000000016132822667240822,
    -.00000000000007540916511956188,
    -.00000000000000036507188831790,
    .00000000000009120937249914984,
    .00000000000018567570959796010,
    -.00000000000003149265065191483,
    -.00000000000009309459495196889,
    .00000000000017914338601329117,
    -.00000000000001302979717330866,
    .00000000000023097385217586939,
    .00000000000023999540484211737,
    .00000000000015393776174455408,
    -.00000000000036870428315837678,
    .00000000000036920375082080089,
    -.00000000000009383417223663699,
    .00000000000009433398189512690,
    .00000000000041481318704258568,
    -.00000000000003792316480209314,
    .00000000000008403156304792424,
    -.00000000000034262934348285429,
    .00000000000043712191957429145,
    -.00000000000010475750058776541,
    -.00000000000011118671389559323,
    .00000000000037549577257259853,
    .00000000000013912841212197565,
    .00000000000010775743037572640,
    .00000000000029391859187648000,
    -.00000000000042790509060060774,
    .00000000000022774076114039555,
    .00000000000010849569622967912,
    -.00000000000023073801945705758,
    .00000000000015761203773969435,
    .00000000000003345710269544082,
    -.00000000000041525158063436123,
    .00000000000032655698896907146,
    -.00000000000044704265010452446,
    .00000000000034527647952039772,
    -.00000000000007048962392109746,
    .00000000000011776978751369214,
    -.00000000000010774341461609578,
    .00000000000021863343293215910,
    .00000000000024132639491333131,
    .00000000000039057462209830700,
    -.00000000000026570679203560751,
    .00000000000037135141919592021,
    -.00000000000017166921336082431,
    -.00000000000028658285157914353,
    -.00000000000023812542263446809,
    .00000000000006576659768580062,
    -.00000000000028210143846181267,
    .00000000000010701931762114254,
    .00000000000018119346366441110,
    .00000000000009840465278232627,
    -.00000000000033149150282752542,
    -.00000000000018302857356041668,
    -.00000000000016207400156744949,
    .00000000000048303314949553201,
    -.00000000000071560553172382115,
    .00000000000088821239518571855,
    -.00000000000030900580513238244,
    -.00000000000061076551972851496,
    .00000000000035659969663347830,
    .00000000000035782396591276383,
    -.00000000000046226087001544578,
    .00000000000062279762917225156,
    .00000000000072838947272065741,
    .00000000000026809646615211673,
    -.00000000000010960825046059278,
    .00000000000002311949383800537,
    -.00000000000058469058005299247,
    -.00000000000002103748251144494,
    -.00000000000023323182945587408,
    -.00000000000042333694288141916,
    -.00000000000043933937969737844,
    .00000000000041341647073835565,
    .00000000000006841763641591466,
    .00000000000047585534004430641,
    .00000000000083679678674757695,
    -.00000000000085763734646658640,
    .00000000000021913281229340092,
    -.00000000000062242842536431148,
    -.00000000000010983594325438430,
    .00000000000065310431377633651,
    -.00000000000047580199021710769,
    -.00000000000037854251265457040,
    .00000000000040939233218678664,
    .00000000000087424383914858291,
    .00000000000025218188456842882,
    -.00000000000003608131360422557,
    -.00000000000050518555924280902,
    .00000000000078699403323355317,
    -.00000000000067020876961949060,
    .00000000000016108575753932458,
    .00000000000058527188436251509,
    -.00000000000035246757297904791,
    -.00000000000018372084495629058,
    .00000000000088606689813494916,
    .00000000000066486268071468700,
    .00000000000063831615170646519,
    .00000000000025144230728376072,
    -.00000000000017239444525614834)

@Strictfp
fun doubleGamma(x: Double): Double {
    return when {
        x >= 6 -> {
            if (x > 171.63) {
                Double.POSITIVE_INFINITY
            } else {
                val u = largeGamma(x)
                expD(u.a, u.b)
            }
        }
        x >= 1.0 + LEFT + x0 -> {
            smallGamma(x)
        }
        x > 1.0e-17 -> {
            smallerGamma(x)
        }
        x > -1.0e-17 -> {
// Raise inexact, only valid in libc
//        if(x != 0.0) {
//            val d = one - tiny
//        }
            1.0 / x
        }
        !x.isFinite() -> {
            x - x
        }
        else -> {
            negGamma(x)
        }
    }
}

private val complexp0 = Complex(1.000000000190015, 0.0)
private val complexp1 = Complex(76.18009172947146, 0.0)
private val complexp2 = Complex(-86.50532032941677, 0.0)
private val complexp3 = Complex(24.01409824083091, 0.0)
private val complexp4 = Complex(-1.231739572450155, 0.0)
private val complexp5 = Complex(1.208650973866179E-3, 0.0)
private val complexp6 = Complex(-5.395239384953E-6, 0.0)

@Strictfp
fun complexGamma(a: Complex): Complex {
    val x = a.real
    val y = a.imaginary
    if (a.real < 0.5) {
        return PI / (complexSin(PI * a) * complexGamma(Complex(1.0 - x, -y)))
    }
    val z1 = Complex(x + 5.5, y)
    val z2 = Complex(x + 0.5, y)
    val res = ((2 * PI).pow(1.0 / 2.0) / a) * (complexp0 +
            complexp1 / Complex(x + 1.0, y) +
            complexp2 / Complex(x + 2.0, y) +
            complexp3 / Complex(x + 3.0, y) +
            complexp4 / Complex(x + 4.0, y) +
            complexp5 / Complex(x + 5.0, y) +
            complexp6 / Complex(x + 6.0, y)) *
            z1.pow(z2) *
            E.pow(-z1)
    return res
}

@Strictfp
private fun largeGamma(xInput: Double): DoubleSet {
    var x = xInput
    val z = 1.0 / (x * x)
    var p = Pa0 + z * (Pa1 + z * (Pa2 + z * (Pa3 + z * (Pa4 + z * (Pa5 + z * (Pa6 + z * Pa7))))))
    p = p / x

    val u = logD(x)
    var ua = u.a - 1.0
    var ub = u.b
    x -= 0.5
    var va = x
    va = doubleTruncate(va)
    val vb = x - va
    val ta = va * ua
    var tb = vb * ua + x * ub

    tb += lns2pi_lo
    tb += p
    ua = lns2pi_hi + tb
    ua += ta
    ub = ta - ua
    ub += lns2pi_hi
    ub += tb

    return DoubleSet(ua, ub)
}

@Strictfp
private fun smallGamma(x: Double): Double {
    var y = x - 1.0
    var ym1 = y - 1.0
    if (y <= 1.0 + (LEFT + x0)) {
        val yy = ratfunGamma(y - x0, 0.0)
        return yy.a + yy.b
    }
    var ra = doubleTruncate(y)
    var yya = ra - 1.0
    y = ym1
    var rb = y - yya
    val yyb = rb

    ym1 = y - 1.0
    while (ym1 > LEFT + x0) {
        val t = ra * yya
        rb = ra * yyb + y * rb
        ra = doubleTruncate(t)
        rb += t - ra

        y = ym1--
        yya--
    }
    val yy = ratfunGamma(y - x0, 0.0)
    y = rb * (yy.a + yy.b) + ra * yy.b
    y += yy.a * ra
    return y
}

@Strictfp
private fun smallerGamma(xInput: Double): Double {
    var x = xInput
    var t: Double
    var d: Double
    val xxa: Double
    var xxb: Double
    if (x < x0 + LEFT) {
        t = doubleTruncate(x)
        d = (t + x) * (x - t)
        t *= t
        xxa = doubleTruncate(t + x)
        xxb = x - xxa
        xxb += t
        xxb += d
        t = 1.0 - x0
        t += x
        d = 1.0 - x0
        d -= t
        d += x
        x = xxa + xxb
    } else {
        xxa = doubleTruncate(x)
        xxb = x - xxa
        t = x - x0
        d = -x0 - t
        d += x
    }
    val r = ratfunGamma(t, d)
    var ra = r.a
    val rb = r.b
    d = doubleTruncate(ra / x)
    ra -= d * xxa
    ra -= d * xxb
    ra += rb
    return d + ra / x
}

@Strictfp
private fun negGamma(x: Double): Double {
    var sgn = 1
    var y: Double
    var z: Double

    y = ceil(x)
    if (y == x) {
        return ((x - x) / 0.0)
    }
    z = y - x
    if (z > 0.5) {
        z = 1.0 - z
    }
    y = 0.5 * y
    if (y == ceil(y)) {
        sgn = -1
    }
    z = if (z < 0.25) {
        sin(PI * z)
    } else {
        cos(PI * (0.5 - z))
    }
    if (x < -170) {
        println("A")
        if (x < -190) {
            println("B")
            val tiny = 1e-300
            return sgn * tiny * tiny
        }
        y - 1.0 - x
        val lg = largeGamma(y)
        var lga = lg.a
        var lgb = lg.b
        val lsine = logD(PI / z)
        lga -= lsine.a
        lgb -= lsine.b
        y = -(lga + lgb)
        z = (y + lga) + lgb
        y = expD(y, z)
        if (sgn < 0) {
            y = -y
        }
        return y
    }
    y = 1.0 - x
    if (1.0 - y == x) {
        y = doubleGamma(y)
    } else {
        y = -x * doubleGamma(-x)
    }
    if (sgn < 0) {
        y = -y
    }
    return PI / (y * z)
}

@Strictfp
private fun ratfunGamma(z: Double, c: Double): DoubleSet {
    var q = Q0 + z * (Q1 + z * (Q2 + z * (Q3 + z * (Q4 + z * (Q5 + z * (Q6 + z * (Q7 + z * Q8)))))))
    var p = P0 + z * (P1 + z * (P2 + z * (P3 + z * P4)))

    p = p / q
    var ta = doubleTruncate(z)
    var tb = (z - ta) + c
    tb *= (ta + z)
    ta *= ta
    q = ta
    ta = doubleTruncate(ta)
    tb += (q - ta)
    var ra = doubleTruncate(p)
    var rb = p - ra
    tb = tb * p + ta * rb + a0_lo
    ta *= ra
    ra = doubleTruncate(ta + a0_hi)
    rb = ((a0_hi - ra) + ta) + tb
    return DoubleSet(ra, rb)
}

private fun doubleTruncate(x: Double): Double {
    val xBits = x.toBits().toULong()
    val highBits = xBits and 0xFFFFFFFF00000000UL
    val lowBits = xBits and 0xFFFFFFFFUL
    return Double.fromBits((highBits or (lowBits and 0xF8000000UL)).toLong())
}

@Strictfp
private fun expD(xInput: Double, cInput: Double): Double {
    var x = xInput
    var c = cInput
    if (x.isNaN()) {
        return x
    }

    val lnhuge = Double.fromBits(0x4086602b15b7ecf2L)
    val lntiny = Double.fromBits(0xc0877af8ebeae354UL.toLong())
    val invln2 = Double.fromBits(0x3ff71547652b82feL)
    val ln2hi = Double.fromBits(0x3fe62e42fee00000L)
    val ln2lo = Double.fromBits(0x3dea39ef35793c76L)
    val p1 = Double.fromBits(0x3fc555555555553eUL.toLong())
    val p2 = Double.fromBits(0xbf66c16c16bebd93UL.toLong())
    val p3 = Double.fromBits(0x3f11566aaf25de2cUL.toLong())
    val p4 = Double.fromBits(0xbebbbd41c5d26bf1UL.toLong())
    val p5 = Double.fromBits(0x3e66376972bea4d0UL.toLong())

    var z: Double
    val k: Int
    val hi: Double
    val lo: Double
    return if (x <= lnhuge) {
        if (x >= lntiny) {
            z = invln2 * x
            k = (z + copysign(.5, x)).toInt()

            hi = (x - k * ln2hi)
            lo = k * ln2lo - c
            x = hi - lo

            z = x * x
            c = x - z * (p1 + z * (p2 + z * (p3 + z * (p4 + z * p5))))
            c = (x * c) / (2.0 - c)

            scalb(1.0 + (hi - (lo - c)), k.toDouble())
        } else {
            if (x.isFinite()) {
                scalb(1.0, -5000.0)
            } else {
                0.0
            }
        }
    } else {
        if (x.isFinite()) scalb(1.0, 5000.0) else x
    }
}

private fun copysign(x: Double, y: Double): Double {
    val xBits = x.toBits().toULong()
    val yBits = y.toBits().toULong()
    val res = (xBits and 0x7fffffff00000000UL) or (yBits and 0x8000000000000000UL)
    return Double.fromBits(res.toLong())
}

@Strictfp
private fun scalb(x: Double, exp: Double) = x * 2.0.pow(exp)

@Strictfp
private fun logD(x: Double): DoubleSet {
    var m = logB(x).toInt()
    var g = ldexp(x, -m)
    if (m == -1022) {
        val a = logB(g).toInt()
        m += a
        g = ldexp(g, -a)
    }
    val j = (N * (g - 1) + 0.5).toInt()
    val F = (1.0 / N) * j + 1
    val f = g - F

    g = 1 / (2 * F + f)
    val u = 2 * f * g
    val v = u * u
    val q = u * v * (A1 + v * (A2 + v * (A3 + v * A4)))

    var u1: Double
    if (m != 0 || j != 0) {
        u1 = u + 513
        u1 -= 513
    } else {
        u1 = doubleTruncate(u)
    }
    var u2 = (2.0 * (f - F * u1) - u1 * f) * g
    u1 += m * logF_head[N] + logF_head[j]
    u2 += logF_tail[j]; u2 += q
    u2 += logF_tail[N] * m
    val resultA = doubleTruncate(u1 + u2)
    val resultB = (u1 - resultA) + u2
    return DoubleSet(resultA, resultB)
}

@Strictfp
private fun ldexp(x: Double, y: Int) = if (y > 0) x * (1 shl y) else x / (1 shl y.absoluteValue)

@Strictfp
private fun logB(x: Double): Double {
    val xBits = x.toBits().toULong()
    val ix = ((xBits and 0xFFFFFFFF00000000UL) shr 32).toInt()
    val lx = (xBits and 0xFFFFFFFFUL).toInt()
    val ixA = ix and 0x7FFFFFFF
    return when {
        (ixA or lx) == 0 -> {
            -1.0 / x.absoluteValue
        }
        ixA >= 0x7FF00000 -> {
            x * x
        }
        ixA < 0x100000 -> {
            val xUp = x * two54
            val xUBits = xUp.toBits().toULong()
            val xBitsMasked = (xUBits shr 32) and 0x7FFFFFFFUL
            (xBitsMasked - 1023UL - 54UL).toDouble()
        }
        else -> {
            ((ixA.toLong() shr 20) - 1023).toDouble()
        }
    }
}

fun doubleBinomial(a: Double, b: Double): Double {
    val row = (if (a < 0) 4 else 0) or (if (b < 0) 2 else 0) or (if (b < a) 1 else 0)
    val isNeg = when (row) {
        0 -> true
        1 -> false
        3 -> true
        4 -> false
        6 -> true
        7 -> false
        else -> throw APLEvalException("Invalid binomial arguments")
    }

    if (!isNeg) {
        return 0.0
    }

    val r1a = a + 1.0
    val r1b = b + 1.0
    val r1ba = r1b - a
    // TODO: Check for illegal values (v < 0 && integer) foreach r1a, r1b, r1ba
    TODO("doubleBinomial not implemented")
}

fun complexBinomial(a: Complex, b: Complex): Complex {
    TODO("Complex binomial not implemented")
}
