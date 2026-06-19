// https://www.itu.int/rec/R-REC-BT.601
// https://www.itu.int/rec/R-REC-BT.709
// https://www.itu.int/rec/R-REC-BT.2020

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (bt.709)

const float beta = 0.018053968510807;
const float alpha = 1.0 + 5.5 * beta;

float bt709_oetf(float L) {
    return L < beta ? 4.5 * L : alpha * pow(L, 0.45) - (alpha - 1.0);
}

vec3 bt709_oetf(vec3 color) {
    return vec3(
        bt709_oetf(color.r),
        bt709_oetf(color.g),
        bt709_oetf(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = bt709_oetf(color.rgb);

    return color;
}
