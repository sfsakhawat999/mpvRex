// from "full" to "limited" signal range

//!PARAM black
//!TYPE float
0.0625

//!PARAM white
//!TYPE float
0.91796875

//!PARAM depth
//!TYPE float
10.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC signal range scaling

float range(float x, float w, float b) {
    return x * (w - b) + b;
}

vec3 range(vec3 x, float w, float b) {
    return x * (w - b) + b;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    float l = exp2(depth);
    float d = l - 1.0;
    float b = l * black / d;
    float w = l * white / d;

    color.rgb = range(color.rgb, w, b);

    return color;
}
