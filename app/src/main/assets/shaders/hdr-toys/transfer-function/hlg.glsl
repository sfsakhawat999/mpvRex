// https://www.arib.or.jp/kikaku/kikaku_hoso/std-b67.html
// https://www.bbc.co.uk/rd/projects/high-dynamic-range
// https://www.itu.int/rec/R-REC-BT.2100

// https://www.itu.int/pub/R-REP-BT.2390
// extended gamma model for Lw is outside 400-2000 cd/m²:
// 1.2 * pow(1.111, log2(Lw / 1000.0)) * pow(0.98, log2(Lamb / 5.0))

//!PARAM reference_white
//!TYPE float
//!MINIMUM 0.0
//!MAXIMUM 1000.0
203.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (hlg)

const vec3 y_coef = vec3(0.2627002120112671, 0.6779980715188708, 0.05930171646986196);

const float Lw   = 1000.0;
const float Lb   = 0.0;
const float Lamb = 5.0;

const float gamma = 1.2 + 0.42 * log(Lw / 1000.0) / log(10.0) - 0.076 * log(Lamb / 5.0) / log(10.0);
const float alpha = Lw;
const float beta  = sqrt(3.0 * pow((Lb / Lw), 1.0 / gamma));

const float a = 0.17883277;
const float b = 1.0 - 4.0 * a;
const float c = 0.5 - a * log(4.0 * a);

float hlg_oetf(float x) {
    return x <= 1.0 / 12.0 ? sqrt(3.0 * x) : a * log(12.0 * x - b) + c;
}

vec3 hlg_oetf(vec3 color) {
    return vec3(
        hlg_oetf(color.r),
        hlg_oetf(color.g),
        hlg_oetf(color.b)
    );
}

vec3 hlg_ootf_inv(vec3 color) {
    float Y = dot(color, y_coef);
    return Y == 0.0 ? vec3(0.0) : pow(Y / alpha, (1.0 - gamma) / gamma) * (color / alpha);
}

vec3 hlg_eotf_inv(vec3 color) {
    return (hlg_oetf(hlg_ootf_inv(color)) - beta) / (1.0 - beta);
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = hlg_eotf_inv(color.rgb * reference_white);

    return color;
}
