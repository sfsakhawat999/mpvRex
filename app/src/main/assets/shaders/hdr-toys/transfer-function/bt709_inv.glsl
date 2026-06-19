// https://www.itu.int/rec/R-REC-BT.601
// https://www.itu.int/rec/R-REC-BT.709
// https://www.itu.int/rec/R-REC-BT.2020

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (bt.709, inverse)

const float beta = 0.018053968510807;
const float alpha = 1.0 + 5.5 * beta;

float bt709_oetf_inv(float V) {
    return V < 4.5 * beta ? V / 4.5 : pow((V + (alpha - 1.0)) / alpha, 1.0 / 0.45);
}

vec3 bt709_oetf_inv(vec3 color) {
    return vec3(
        bt709_oetf_inv(color.r),
        bt709_oetf_inv(color.g),
        bt709_oetf_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = bt709_oetf_inv(color.rgb);

    return color;
}
