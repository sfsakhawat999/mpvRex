//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Blackmagic Film Gen5, inverse)

float blackmagic_filmgen5_inv(float x) {
    const float A = 0.08692876065491224;
    const float B = 0.005494072432257808;
    const float C = 0.5300133392291939;
    const float D = 8.283605932402494;
    const float E = 0.09246575342465753;
    const float LIN_CUT = 0.005;
    const float LOG_CUT = D * LIN_CUT + E;

    if (x < LOG_CUT) {
        return (x - E) / D;
    }
    return exp((x - C) / A) - B;
}

vec3 blackmagic_filmgen5_inv(vec3 color) {
    return vec3(
        blackmagic_filmgen5_inv(color.r),
        blackmagic_filmgen5_inv(color.g),
        blackmagic_filmgen5_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = blackmagic_filmgen5_inv(color.rgb);

    return color;
}
