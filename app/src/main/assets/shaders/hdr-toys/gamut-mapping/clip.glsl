// RGB to RGB conversion, includes chromatic adaptation transform
// All coordinates are based on the CIE 1931 2° chromaticity diagram

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC gamut mapping (clip)

// You can use custom chromaticity here.
// Example: BT.709 with a D93 white point: Chromaticity(BT709.r, BT709.g, BT709.b, D93)
// You can also define custom coordinates: Chromaticity(vec2(0.7347, 0.2653), BT709.g, BT709.b, D65)

#define from    BT2020
#define to      BT709

// White points of standard illuminants
// https://en.wikipedia.org/wiki/Standard_illuminant#White_points_of_standard_illuminants

const vec2 A    = vec2(0.44757, 0.40745);
const vec2 B    = vec2(0.34842, 0.35161);
const vec2 C    = vec2(0.31006, 0.31616);
const vec2 D50  = vec2(0.34567, 0.35850);
const vec2 D55  = vec2(0.33242, 0.34743);
const vec2 D65  = vec2(0.31271, 0.32902);
const vec2 D75  = vec2(0.29902, 0.31485);
const vec2 D93  = vec2(0.28315, 0.29711);
const vec2 E    = vec2(1.0/3.0, 1.0/3.0);
const vec2 F2   = vec2(0.37208, 0.37529);
const vec2 F7   = vec2(0.31292, 0.32933);
const vec2 F11  = vec2(0.38052, 0.37713);
const vec2 DCI  = vec2(0.31400, 0.35100);
// It is also known as D60
const vec2 ACES = vec2(0.32168, 0.33767);
// Colour Matching Between OLED and CRT
// https://www.sony.jp/products/catalog/FUN_WhitePaper_OLED_ColorMatching_V1_00.pdf
const vec2 BRAVIA = vec2(0.3067, 0.318);

// https://en.wikipedia.org/wiki/Standard_illuminant#Illuminant_series_D
vec2 CIE_D(float T) {
    // Compensate for the loss caused by the accuracy difference between the old and new standards
    // c2 = 1.4387768775039337
    // https://en.wikipedia.org/wiki/Planckian_locus#Planckian_locus_in_the_XYZ_color_space
    T = (T * 1.4388) / 1.438;

    // This formula is applicable to temperatures ranging from 4000K to 25000K
    T = clamp(T, 4000.0, 25000.0);

    float t1 = 1000.0 / T;
    float t2 = t1 * t1;
    float t3 = t1 * t2;

    float x =
        T <= 7000.0
            ? 0.244063 + 0.09911 * t1 + 2.9678 * t2 - 4.607 * t3
            : 0.23704 + 0.24748 * t1 + 1.9018 * t2 - 2.0064 * t3;

    // Daylight locus
    float y = -0.275 + 2.87 * x - 3.0 * x * x;

    return vec2(x, y);
}

// https://en.wikipedia.org/wiki/Planckian_locus#Approximation
vec2 Kang(float T) {
    // This formula is applicable to temperatures ranging from 1667K to 25000K
    T = clamp(T, 1667.0, 25000.0);

    float t1 = 1000.0 / T;
    float t2 = t1 * t1;
    float t3 = t1 * t2;

    float x =
        T <= 4000.0
            ? -0.2661239 * t3 - 0.234358 * t2 + 0.8776956 * t1 + 0.17991
            : -3.0258469 * t3 + 2.1070379 * t2 + 0.2226347 * t1 + 0.24039;

    float x2 = x * x;
    float x3 = x2 * x;

    float y =
        T <= 2222.0
            ? -1.1063814 * x3 - 1.3481102 * x2 - 2.18555832 * x - 0.20219683
            : T <= 4000.0
            ? -0.9549476 * x3 - 1.37418593 * x2 - 2.09137015 * x - 0.16748867
            : 3.081758 * x3 - 5.8733867 * x2 + 3.75112997 * x - 0.37001483;

    return vec2(x, y);
}

// Chromaticities
// https://www.itu.int/rec/T-REC-H.273
// https://github.com/colour-science/colour/tree/develop/colour/models/rgb/datasets

struct Chromaticity {
    vec2 r, g, b, w;
};

// ITU-R BT.2020, ITU-R BT.2100
const Chromaticity BT2020 = Chromaticity(
    vec2(0.708, 0.292),
    vec2(0.170, 0.797),
    vec2(0.131, 0.046),
    D65
);

// ITU-R BT.709, IEC 61966-2-1 (sRGB)
const Chromaticity BT709 = Chromaticity(
    vec2(0.64, 0.33),
    vec2(0.30, 0.60),
    vec2(0.15, 0.06),
    D65
);

// ITU-R BT.601 (525 lines), SMPTE ST 240
const Chromaticity BT601_525 = Chromaticity(
    vec2(0.630, 0.340),
    vec2(0.310, 0.595),
    vec2(0.155, 0.070),
    D65
);

// ITU-R BT.601 (625 lines), BT.470 (B/G), EBU 3213-E
const Chromaticity BT601_625 = Chromaticity(
    vec2(0.64, 0.33),
    vec2(0.29, 0.60),
    vec2(0.15, 0.06),
    D65
);

// ITU-R BT.470 (M)
const Chromaticity BT470m = Chromaticity(
    vec2(0.67, 0.33),
    vec2(0.21, 0.71),
    vec2(0.14, 0.08),
    C
);

// P3-DCI (Theater)
const Chromaticity P3DCI = Chromaticity(
    vec2(0.680, 0.320),
    vec2(0.265, 0.690),
    vec2(0.150, 0.060),
    DCI
);

// P3-D65 (Display)
const Chromaticity P3D65 = Chromaticity(
    P3DCI.r,
    P3DCI.g,
    P3DCI.b,
    D65
);

// P3-D60 (ACES Cinema)
const Chromaticity P3D60 = Chromaticity(
    P3DCI.r,
    P3DCI.g,
    P3DCI.b,
    ACES
);

// ITU-T H.273 (Generic film)
const Chromaticity H273_8 = Chromaticity(
    vec2(0.681, 0.319),
    vec2(0.243, 0.692),
    vec2(0.145, 0.049),
    C
);

// ITU-T H.273 (No corresponding industry specification identified)
const Chromaticity H273_22 = Chromaticity(
    vec2(0.630, 0.340),
    vec2(0.295, 0.605),
    vec2(0.155, 0.077),
    D65
);

// CIE RGB (CIE 1931 color space)
const Chromaticity CIERGB = Chromaticity(
    vec2(0.73474284, 0.26525716),
    vec2(0.27377903, 0.7174777),
    vec2(0.16655563, 0.00891073),
    E
);

// CIE XYZ (CIE 1931 color space)
const Chromaticity XYZ = Chromaticity(
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(0.0, 0.0),
    E
);

// CIE XYZ (CIE 1931 color space, D65 whitepoint)
const Chromaticity XYZD65 = Chromaticity(
    XYZ.r,
    XYZ.g,
    XYZ.b,
    D65
);

// CIE XYZ (CIE 1931 color space, D50 whitepoint)
const Chromaticity XYZD50 = Chromaticity(
    XYZ.r,
    XYZ.g,
    XYZ.b,
    D50
);

// Grayscale, Monochrome
const Chromaticity GRAY = Chromaticity(
    vec2(0.0, 1.0),
    vec2(0.0, 1.0),
    vec2(0.0, 1.0),
    E
);

// Adobe RGB (1998)
const Chromaticity AdobeRGB = Chromaticity(
    vec2(0.64, 0.33),
    vec2(0.21, 0.71),
    vec2(0.15, 0.06),
    D65
);

// Adobe Wide Gamut RGB
const Chromaticity AdobeWideGamutRGB = Chromaticity(
    vec2(0.7347, 0.2653),
    vec2(0.1152, 0.8264),
    vec2(0.1566, 0.0177),
    D50
);

// ROMM (ProPhoto RGB)
const Chromaticity ROMM = Chromaticity(
    vec2(0.734699, 0.265301),
    vec2(0.159597, 0.840403),
    vec2(0.036598, 0.000105),
    D50
);

// AP0 (ACES 2065-1)
const Chromaticity AP0 = Chromaticity(
    vec2(0.7347,  0.2653),
    vec2(0.0000,  1.0000),
    vec2(0.0001, -0.0770),
    ACES
);

// AP1 (ACEScg, cc, cct, proxy)
const Chromaticity AP1 = Chromaticity(
    vec2(0.713, 0.293),
    vec2(0.165, 0.830),
    vec2(0.128, 0.044),
    ACES
);

// ARRI Wide Gamut 3
const Chromaticity AWG3 = Chromaticity(
    vec2(0.684,   0.313),
    vec2(0.221,   0.848),
    vec2(0.0861, -0.102),
    D65
);

// ARRI Wide Gamut 4
const Chromaticity AWG4 = Chromaticity(
    vec2(0.7347,  0.2653),
    vec2(0.1424,  0.8576),
    vec2(0.0991, -0.0308),
    D65
);

// RED Wide Gamut RGB
const Chromaticity RWG = Chromaticity(
    vec2(0.780308,  0.304253),
    vec2(0.121595,  1.493994),
    vec2(0.095612, -0.084589),
    D65
);

// DaVinci Wide Gamut
const Chromaticity DWG = Chromaticity(
    vec2(0.8000,  0.3130),
    vec2(0.1682,  0.9877),
    vec2(0.0790, -0.1155),
    D65
);

// FilmLight E-Gamut
const Chromaticity EGAMUT = Chromaticity(
    vec2(0.8000,  0.3177),
    vec2(0.1800,  0.9000),
    vec2(0.0650, -0.0805),
    D65
);

// FilmLight E-Gamut 2
const Chromaticity EGAMUT2 = Chromaticity(
    vec2(0.8300,  0.3100),
    vec2(0.1500,  0.9500),
    vec2(0.0650, -0.0805),
    D65
);

// FUJIFILM F-Gamut C
const Chromaticity FGAMUTC = Chromaticity(
    vec2(0.7347,  0.2653),
    vec2(0.0263,  0.9737),
    vec2(0.1173, -0.0224),
    D65
);

// Sony S-Gamut3/S-Gamut
const Chromaticity SGAMUT = Chromaticity(
    vec2(0.73,  0.280),
    vec2(0.14,  0.855),
    vec2(0.10, -0.050),
    D65
);

// Sony S-Gamut.Cine
const Chromaticity SGAMUTCINE = Chromaticity(
    vec2(0.766,  0.275),
    vec2(0.225,  0.800),
    vec2(0.089, -0.087),
    D65
);

// Canon Cinema Gamut
const Chromaticity CINEMA_GAMUT = Chromaticity(
    vec2(0.74,  0.27),
    vec2(0.17,  1.14),
    vec2(0.08, -0.10),
    D65
);

// Panasonic V-Gamut
const Chromaticity VGAMUT = Chromaticity(
    vec2(0.730,  0.28),
    vec2(0.165,  0.84),
    vec2(0.100, -0.03),
    D65
);

// DJI D-Gamut
const Chromaticity DGAMUT = Chromaticity(
    vec2(0.71,  0.31),
    vec2(0.21,  0.88),
    vec2(0.09, -0.08),
    D65
);

// Apple Wide Gamut
const Chromaticity AppleWideGamut = Chromaticity(
    vec2(0.725,  0.301),
    vec2(0.221,  0.814),
    vec2(0.068, -0.076),
    D65
);

// Chromatic adaptation transform
// https://en.wikipedia.org/wiki/LMS_color_space

// It is also known as von Kries
const mat3 HPE = mat3(
     0.40024,  0.70760, -0.08081,
    -0.22630,  1.16532,  0.04570,
     0.00000,  0.00000,  0.91822
);

const mat3 Bradford = mat3(
     0.8951,  0.2664, -0.1614,
    -0.7502,  1.7135,  0.0367,
     0.0389, -0.0685,  1.0296
);

const mat3 CAT97 = mat3(
     0.8562,  0.3372, -0.1934,
    -0.8360,  1.8327,  0.0033,
     0.0357, -0.0469,  1.0112
);

const mat3 CAT02 = mat3(
     0.7328,  0.4296, -0.1624,
    -0.7036,  1.6975,  0.0061,
     0.0030,  0.0136,  0.9834
);

const mat3 CAT16 = mat3(
     0.401288,  0.650173, -0.051461,
    -0.250268,  1.204414,  0.045854,
    -0.002079,  0.048952,  0.953127
);

// Other constants

const mat3 Identity3 = mat3(
    1.0, 0.0, 0.0,
    0.0, 1.0, 0.0,
    0.0, 0.0, 1.0
);

const mat3 SingularY3 = mat3(
    0.0, 1.0, 0.0,
    0.0, 1.0, 0.0,
    0.0, 1.0, 0.0
);

// http://www.brucelindbloom.com/Eqn_xyY_to_XYZ.html
vec3 xyY_to_XYZ(vec3 xyY) {
    float x = xyY.x;
    float y = xyY.y;
    float Y = xyY.z;

    float multiplier = Y / max(y, 1e-6);

    float z = 1.0 - x - y;
    float X = x * multiplier;
    float Z = z * multiplier;

    return vec3(X, Y, Z);
}

// http://www.brucelindbloom.com/Eqn_XYZ_to_xyY.html
vec3 XYZ_to_xyY(vec3 XYZ) {
    float X = XYZ.x;
    float Y = XYZ.y;
    float Z = XYZ.z;

    float divisor = X + Y + Z;
    if (divisor == 0.0) divisor = 1e-6;

    float x = X / divisor;
    float y = Y / divisor;

    return vec3(x, y, Y);
}

// http://www.brucelindbloom.com/Eqn_RGB_XYZ_Matrix.html
mat3 RGB_to_XYZ(Chromaticity C) {
    if (C == GRAY)
        return Identity3;

    vec3 r = xyY_to_XYZ(vec3(C.r, 1.0));
    vec3 g = xyY_to_XYZ(vec3(C.g, 1.0));
    vec3 b = xyY_to_XYZ(vec3(C.b, 1.0));
    vec3 w = xyY_to_XYZ(vec3(C.w, 1.0));

    mat3 xyz = transpose(mat3(r, g, b));

    vec3 scale = w * inverse(xyz);
    mat3 scale_diag = mat3(
        scale.x, 0.0, 0.0,
        0.0, scale.y, 0.0,
        0.0, 0.0, scale.z
    );

    return scale_diag * xyz;
}

mat3 XYZ_to_RGB(Chromaticity C) {
    if (C == GRAY)
        return SingularY3;

    return inverse(RGB_to_XYZ(C));
}

// http://www.brucelindbloom.com/Eqn_ChromAdapt.html
mat3 adaptation(vec2 w1, vec2 w2, mat3 cat) {
    vec3 src_xyz = xyY_to_XYZ(vec3(w1, 1.0));
    vec3 dst_xyz = xyY_to_XYZ(vec3(w2, 1.0));

    vec3 src_lms = src_xyz * cat;
    vec3 dst_lms = dst_xyz * cat;

    vec3 scale = dst_lms / src_lms;
    mat3 scale_diag = mat3(
        scale.x, 0.0, 0.0,
        0.0, scale.y, 0.0,
        0.0, 0.0, scale.z
    );

    return cat * scale_diag * inverse(cat);
}

// CAM16 uses CAT16 as cat and equal-energy illuminant (E) as wt.
// https://www.researchgate.net/publication/318152296_Comprehensive_color_solutions_CAM16_CAT16_and_CAM16-UCS
// Android uses Bradford as cat and D50 as wt.
// https://android.googlesource.com/platform/frameworks/base/+/master/graphics/java/android/graphics/ColorSpace.java
mat3 adaptation_two_step(vec2 w1, vec2 w2, vec2 wt, mat3 cat) {
    return adaptation(w1, wt, cat) * adaptation(wt, w2, cat);
}

mat3 adaptation_two_step(vec2 w1, vec2 w2) {
    return adaptation_two_step(w1, w2, E, CAT16);
}

mat3 RGB_to_RGB(Chromaticity c1, Chromaticity c2) {
    mat3 m = Identity3;
    if (c1 != c2) {
        m *= RGB_to_XYZ(c1);
        if (c1.w != c2.w) {
            m *= adaptation_two_step(c1.w, c2.w);
        }
        m *= XYZ_to_RGB(c2);
    }
    return m;
}

vec3 RGB_to_RGB(vec3 c, Chromaticity c1, Chromaticity c2) {
    return c * RGB_to_RGB(c1, c2);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_RGB(color.rgb, from, to);

    return color;
}
