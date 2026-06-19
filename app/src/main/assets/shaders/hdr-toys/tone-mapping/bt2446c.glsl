// ITU-R BT.2446 Conversion Method C
// https://www.itu.int/pub/R-REP-BT.2446

//!PARAM reference_white
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1000.0
203.0

//!PARAM alpha
//!TYPE float
//!MINIMUM 0.00
//!MAXIMUM 0.33
0.04

//!PARAM sigma
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1.0
0.33

//!HOOK OUTPUT
//!BIND HOOKED
//!WHEN alpha
//!DESC tone mapping (bt.2446c, crosstalk)

// The crosstalk matrix is applied such that saturations of linear signals are reduced to achromatic to
// avoid hue changes caused by clipping of compressed highlight parts.

vec3 crosstalk(vec3 x, float a) {
    float b = 1.0 - 2.0 * a;
    mat3 transform = mat3(
        b, a, a,
        a, b, a,
        a, a, b
    );
    return x * transform;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = crosstalk(color.rgb, alpha);

    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!WHEN sigma
//!DESC tone mapping (bt.2446c, chroma correction)

// Optional processing of chroma correction above HDR Reference White

// In SDR production, highlight parts are sometimes intentionally expressed as white. The processing
// described in this section is optionally used to shift chroma above HDR Reference White to achromatic
// when the converted SDR content requires a degree of consistency for SDR production content. This
// processing is applied as needed before the tone-mapping processing.

vec3 RGB_to_XYZ(vec3 RGB) {
    return RGB * mat3(
        0.6369580483012914, 0.14461690358620832,  0.1688809751641721,
        0.2627002120112671, 0.6779980715188708,   0.05930171646986196,
        0.000000000000000,  0.028072693049087428, 1.060985057710791
    );
}

vec3 XYZ_to_RGB(vec3 XYZ) {
    return XYZ * mat3(
         1.716651187971268,  -0.355670783776392, -0.253366281373660,
        -0.666684351832489,   1.616481236634939,  0.0157685458139111,
         0.017639857445311,  -0.042770613257809,  0.942103121235474
    );
}

float cbrt(float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

const float delta = 6.0 / 29.0;
const float deltac = delta * 2.0 / 3.0;

float f(float x) {
    return x > pow(delta, 3.0) ?
        cbrt(x) :
        deltac + x / (3.0 * pow(delta, 2.0));
}

vec3 f(vec3 x) {
    return vec3(f(x.x), f(x.y), f(x.z));
}

float f_inv(float x) {
    return x > delta ?
        pow(x, 3.0) :
        (x - deltac) * (3.0 * pow(delta, 2.0));
}

vec3 f_inv(vec3 x) {
    return vec3(f_inv(x.x), f_inv(x.y), f_inv(x.z));
}

const vec3 XYZn = vec3(0.95047, 1.00000, 1.08883);

vec3 XYZ_to_Lab(vec3 XYZ) {
    XYZ = f(XYZ / XYZn);

    float X = XYZ.x;
    float Y = XYZ.y;
    float Z = XYZ.z;

    float L = 116.0 * Y - 16.0;
    float a = 500.0 * (X - Y);
    float b = 200.0 * (Y - Z);

    return vec3(L, a, b);
}

vec3 Lab_to_XYZ(vec3 Lab) {
    float L = Lab.x;
    float a = Lab.y;
    float b = Lab.z;

    float Y = (L + 16.0) / 116.0;
    float X = Y + a / 500.0;
    float Z = Y - b / 200.0;

    vec3 XYZ = f_inv(vec3(X, Y, Z)) * XYZn;

    return XYZ;
}

vec3 RGB_to_Lab(vec3 color) {
    color  = RGB_to_XYZ(color);
    color  = XYZ_to_Lab(color);
    return color;
}

vec3 Lab_to_RGB(vec3 color) {
    color  = Lab_to_XYZ(color);
    color  = XYZ_to_RGB(color);
    return color;
}

const float epsilon = 1e-6;

vec3 Lab_to_LCh(vec3 Lab) {
    float L = Lab.x;
    float a = Lab.y;
    float b = Lab.z;

    float C = length(vec2(a, b));
    float h = (abs(a) < epsilon && abs(b) < epsilon) ? 0.0 : atan(b, a);

    return vec3(L, C, h);
}

vec3 LCh_to_Lab(vec3 LCh) {
    float L = LCh.x;
    float C = LCh.y;
    float h = LCh.z;

    C = max(C, 0.0);
    float a = C * cos(h);
    float b = C * sin(h);

    return vec3(L, a, b);
}

float chroma_correction(float L, float Lref, float Lmax, float sigma) {
    return L <= Lref ? 1.0 : max(1.0 - sigma * (L - Lref) / (Lmax - Lref), 0.0);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    const float Lref = RGB_to_Lab(vec3(1.0)).x;
    const float Lmax = RGB_to_Lab(vec3(1000.0 / reference_white)).x;

    color.rgb = RGB_to_Lab(color.rgb);
    color.rgb = Lab_to_LCh(color.rgb);
    color.y  *= chroma_correction(color.x, Lref, Lmax, sigma);
    color.rgb = LCh_to_Lab(color.rgb);
    color.rgb = Lab_to_RGB(color.rgb);

    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC tone mapping (bt.2446c)

vec3 RGB_to_XYZ(vec3 RGB) {
    return RGB * mat3(
        0.6369580483012914, 0.14461690358620832,  0.1688809751641721,
        0.2627002120112671, 0.6779980715188708,   0.05930171646986196,
        0.000000000000000,  0.028072693049087428, 1.060985057710791
    );
}

vec3 XYZ_to_RGB(vec3 XYZ) {
    return XYZ * mat3(
         1.716651187971268,  -0.355670783776392, -0.253366281373660,
        -0.666684351832489,   1.616481236634939,  0.0157685458139111,
         0.017639857445311,  -0.042770613257809,  0.942103121235474
    );
}

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

const float ip = 0.58535;   // linear length
const float k1 = 0.83802;   // linear strength
const float k3 = 0.74204;   // shoulder strength

float f(float Y, float k1, float k3, float ip) {
    ip /= k1;
    float k2 = (k1 * ip) * (1.0 - k3);
    float k4 = (k1 * ip) - (k2 * log(1.0 - k3));
    return Y < ip ? Y * k1 : log((Y / ip) - k3) * k2 + k4;
}

float curve(float x) {
    return f(x, k1, k3, ip);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = RGB_to_XYZ(color.rgb);
    color.rgb = XYZ_to_xyY(color.rgb);
    color.z   = curve(color.z);
    color.rgb = xyY_to_XYZ(color.rgb);
    color.rgb = XYZ_to_RGB(color.rgb);

    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!WHEN alpha
//!DESC tone mapping (bt.2446c, inverse crosstalk)

// The inverse crosstalk matrix is applied to ensure that the original hues of input HDR images are
// recovered.

vec3 crosstalk_inv(vec3 x, float a) {
    float b = 1.0 - a;
    float c = 1.0 / (1.0 - 3.0 * a);
    mat3 transform = mat3(
        b, -a, -a,
        -a, b, -a,
        -a, -a, b
    );
    return x * transform * c;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = crosstalk_inv(color.rgb, alpha);

    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC tone mapping (bt.2446c, signal scaling)

// Handling 109% range (super-whites) and black point compensation

float f_slope(float x0, float y0, float x1, float y1) {
    float num = (y1 - y0);
    float den = (x1 - x0);
    return abs(den) < 1e-6 ? 1.0 : num / den;
}

float f_intercept(float slope, float x0, float y0) {
    return y0 - slope * x0;
}

float f_linear(float x, float slope, float intercept) {
    return slope * x + intercept;
}

float f_scale(float x, float x0, float y0, float x1, float y1) {
    float slope = f_slope(x0, y0, x1, y1);
    float intercept = f_intercept(slope, x0, y0);
    return f_linear(x, slope, intercept);
}

vec3 f_scale(vec3 x, float x0, float y0, float x1, float y1) {
    return vec3(
        f_scale(x.x, x0, y0, x1, y1),
        f_scale(x.y, x0, y0, x1, y1),
        f_scale(x.z, x0, y0, x1, y1)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = f_scale(color.rgb, 0.0, 0.001, 1019.0 / 940.0, 1.0);

    return color;
}
