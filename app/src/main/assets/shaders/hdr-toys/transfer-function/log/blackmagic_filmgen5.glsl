//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Blackmagic Film Gen5)

float blackmagic_filmgen5(float x) {
    const float A = 0.08692876065491224;
    const float B = 0.005494072432257808;
    const float C = 0.5300133392291939;
    const float D = 8.283605932402494;
    const float E = 0.09246575342465753;
    const float LIN_CUT = 0.005;

    if (x < LIN_CUT) {
        return D * x + E;
    }
    return A * log(x + B) + C;
}

vec3 blackmagic_filmgen5(vec3 color) {
    return vec3(
        blackmagic_filmgen5(color.r),
        blackmagic_filmgen5(color.g),
        blackmagic_filmgen5(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = blackmagic_filmgen5(color.rgb);

    return color;
}
