// https://www.itu.int/rec/R-REC-BT.1886

//!PARAM contrast_ratio
//!TYPE float
//!MINIMUM 10.0
//!MAXIMUM 100000000.0
1000.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (bt.1886, inverse)

float bt1886_eotf(float V, float gamma, float Lw, float Lb) {
    float a = pow(pow(Lw, 1.0 / gamma) - pow(Lb, 1.0 / gamma), gamma);
    float b = pow(Lb, 1.0 / gamma) / (pow(Lw, 1.0 / gamma) - pow(Lb, 1.0 / gamma));
    float L = a * pow(max(V + b, 0.0), gamma);
    return L;
}

vec3 bt1886_eotf(vec3 color, float gamma, float Lw, float Lb) {
    return vec3(
        bt1886_eotf(color.r, gamma, Lw, Lb),
        bt1886_eotf(color.g, gamma, Lw, Lb),
        bt1886_eotf(color.b, gamma, Lw, Lb)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = bt1886_eotf(color.rgb, 2.4, 1.0, 1.0 / contrast_ratio);

    return color;
}
