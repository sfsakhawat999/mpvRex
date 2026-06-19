// https://www.itu.int/rec/R-REC-BT.1886

//!PARAM contrast_ratio
//!TYPE float
//!MINIMUM 10.0
//!MAXIMUM 100000000.0
1000.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (bt.1886)

float bt1886_eotf_inv(float L, float gamma, float Lw, float Lb) {
    float a = pow(pow(Lw, 1.0 / gamma) - pow(Lb, 1.0 / gamma), gamma);
    float b = pow(Lb, 1.0 / gamma) / (pow(Lw, 1.0 / gamma) - pow(Lb, 1.0 / gamma));
    float V = pow(max(L / a, 0.0), 1.0 / gamma) - b;
    return V;
}

vec3 bt1886_eotf_inv(vec3 color, float gamma, float Lw, float Lb) {
    return vec3(
        bt1886_eotf_inv(color.r, gamma, Lw, Lb),
        bt1886_eotf_inv(color.g, gamma, Lw, Lb),
        bt1886_eotf_inv(color.b, gamma, Lw, Lb)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = bt1886_eotf_inv(color.rgb, 2.4, 1.0, 1.0 / contrast_ratio);

    return color;
}
