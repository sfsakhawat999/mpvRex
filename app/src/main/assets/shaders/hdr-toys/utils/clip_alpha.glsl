//!HOOK OUTPUT
//!BIND HOOKED
//!DESC clip code value (black)

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.a = 1.0;

    return color;
}
