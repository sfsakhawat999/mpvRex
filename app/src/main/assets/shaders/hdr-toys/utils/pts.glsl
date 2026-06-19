// Visualize PTS (Presentation Timestamp)

//!PARAM PTS
//!TYPE float
0.0

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC PTS visualization

// 3x5 bitmap font rendering
// Each glyph is 3 columns x 5 rows, packed into a uint (bits 0-14).
// Bit index = row * 3 + col, row 0 = top, col 0 = leftmost.

const float CHAR_W = 3.0;
const float CHAR_H = 5.0;
const float SPACING = 1.0;
const float MARGIN = 8.0;
const float PAD = 2.0;
const float SCALE = 4.0;

// 0-9
const uint FONT_0 = 0x7B6Fu;
const uint FONT_1 = 0x749Au;
const uint FONT_2 = 0x73E7u;
const uint FONT_3 = 0x79E7u;
const uint FONT_4 = 0x49EDu;
const uint FONT_5 = 0x79CFu;
const uint FONT_6 = 0x7BCFu;
const uint FONT_7 = 0x4927u;
const uint FONT_8 = 0x7BEFu;
const uint FONT_9 = 0x79EFu;

// A-Z
const uint FONT_A = 0x5BEFu;
const uint FONT_B = 0x3AEBu;
const uint FONT_C = 0x724Fu;
const uint FONT_D = 0x3B6Bu;
const uint FONT_E = 0x72CFu;
const uint FONT_F = 0x12CFu;
const uint FONT_G = 0x7B4Fu;
const uint FONT_H = 0x5BEDu;
const uint FONT_I = 0x7497u;
const uint FONT_J = 0x7B24u;
const uint FONT_K = 0x5AEDu;
const uint FONT_L = 0x7249u;
const uint FONT_M = 0x5BFDu;
const uint FONT_N = 0x5B6Fu;
const uint FONT_O = 0x7B6Fu;
const uint FONT_P = 0x13EFu;
const uint FONT_Q = 0x49EFu;
const uint FONT_R = 0x5AEFu;
const uint FONT_S = 0x388Eu;
const uint FONT_T = 0x2497u;
const uint FONT_U = 0x7B6Du;
const uint FONT_V = 0x256Du;
const uint FONT_W = 0x5FEDu;
const uint FONT_X = 0x5AADu;
const uint FONT_Y = 0x24ADu;
const uint FONT_Z = 0x72A7u;

// Special characters
const uint FONT_COLON = 0x0410u;
const uint FONT_DOT   = 0x2000u;
const uint FONT_MINUS = 0x01C0u;
const uint FONT_SPACE = 0x0000u;
const uint FONT_TOFU  = 0x7FFFu;

// ASCII code constants
const int CH_SPACE = 32;
const int CH_MINUS = 45;
const int CH_DOT   = 46;
const int CH_0     = 48;
const int CH_1     = 49;
const int CH_2     = 50;
const int CH_3     = 51;
const int CH_4     = 52;
const int CH_5     = 53;
const int CH_6     = 54;
const int CH_7     = 55;
const int CH_8     = 56;
const int CH_9     = 57;
const int CH_COLON = 58;
const int CH_A     = 65;
const int CH_B     = 66;
const int CH_C     = 67;
const int CH_D     = 68;
const int CH_E     = 69;
const int CH_F     = 70;
const int CH_G     = 71;
const int CH_H     = 72;
const int CH_I     = 73;
const int CH_J     = 74;
const int CH_K     = 75;
const int CH_L     = 76;
const int CH_M     = 77;
const int CH_N     = 78;
const int CH_O     = 79;
const int CH_P     = 80;
const int CH_Q     = 81;
const int CH_R     = 82;
const int CH_S     = 83;
const int CH_T     = 84;
const int CH_U     = 85;
const int CH_V     = 86;
const int CH_W     = 87;
const int CH_X     = 88;
const int CH_Y     = 89;
const int CH_Z     = 90;

// Lookup glyph bitmap by ASCII code.
uint get_glyph(int ch) {
    if (ch == CH_SPACE) return FONT_SPACE;
    if (ch == CH_MINUS) return FONT_MINUS;
    if (ch == CH_DOT)   return FONT_DOT;
    if (ch == CH_COLON) return FONT_COLON;
    if (ch == CH_0) return FONT_0;
    if (ch == CH_1) return FONT_1;
    if (ch == CH_2) return FONT_2;
    if (ch == CH_3) return FONT_3;
    if (ch == CH_4) return FONT_4;
    if (ch == CH_5) return FONT_5;
    if (ch == CH_6) return FONT_6;
    if (ch == CH_7) return FONT_7;
    if (ch == CH_8) return FONT_8;
    if (ch == CH_9) return FONT_9;
    if (ch == CH_A) return FONT_A;
    if (ch == CH_B) return FONT_B;
    if (ch == CH_C) return FONT_C;
    if (ch == CH_D) return FONT_D;
    if (ch == CH_E) return FONT_E;
    if (ch == CH_F) return FONT_F;
    if (ch == CH_G) return FONT_G;
    if (ch == CH_H) return FONT_H;
    if (ch == CH_I) return FONT_I;
    if (ch == CH_J) return FONT_J;
    if (ch == CH_K) return FONT_K;
    if (ch == CH_L) return FONT_L;
    if (ch == CH_M) return FONT_M;
    if (ch == CH_N) return FONT_N;
    if (ch == CH_O) return FONT_O;
    if (ch == CH_P) return FONT_P;
    if (ch == CH_Q) return FONT_Q;
    if (ch == CH_R) return FONT_R;
    if (ch == CH_S) return FONT_S;
    if (ch == CH_T) return FONT_T;
    if (ch == CH_U) return FONT_U;
    if (ch == CH_V) return FONT_V;
    if (ch == CH_W) return FONT_W;
    if (ch == CH_X) return FONT_X;
    if (ch == CH_Y) return FONT_Y;
    if (ch == CH_Z) return FONT_Z;
    return FONT_TOFU;
}

// Test if pixel is lit for glyph at local position p (0,0 = top-left).
bool glyph_pixel(uint glyph, vec2 p) {
    if (p.x < 0.0 || p.x >= CHAR_W || p.y < 0.0 || p.y >= CHAR_H) return false;
    uint bit = uint(p.y) * 3u + uint(p.x);
    return (glyph & (1u << bit)) != 0u;
}

// Extract a decimal digit from an integer by position (0 = ones, 1 = tens, etc.)
uint extract_digit(uint value, uint pos) {
    uint d = 1u;
    for (uint i = 0u; i < pos; i++) d *= 10u;
    return (value / d) % 10u;
}

// Draw background overlay behind text area.
vec4 draw_background(vec2 origin, vec2 px, float width) {
    vec2 local = (px - origin) / SCALE;
    if (local.x >= -PAD && local.x <= width + PAD &&
        local.y >= -PAD && local.y <= CHAR_H + PAD)
        return vec4(0.0, 0.0, 0.0, 0.7);
    return vec4(0.0);
}

// Draw a single character at pixel-space origin.
// Returns vec4(rgb, a) and advances cursor (cx) by one character width.
// cx is in local (unscaled) units.
vec4 draw_char(int ch, vec2 origin, vec2 px, inout float cx) {
    vec2 local = (px - origin) / SCALE;
    vec2 cp = local - vec2(cx, 0.0);
    cx += CHAR_W + SPACING;
    if (cp.x >= 0.0 && cp.x < CHAR_W && cp.y >= 0.0 && cp.y < CHAR_H) {
        if (glyph_pixel(get_glyph(ch), cp))
            return vec4(1.0, 1.0, 1.0, 1.0);
    }
    return vec4(0.0);
}

// Draw a float number starting at cursor cx.
// Returns foreground vec4 if any digit pixel is hit.
vec4 draw_number(float value, vec2 origin, vec2 px, inout float cx) {
    bool negative = value < 0.0;
    float abs_val = min(abs(value), 99999.99);

    uint int_part = uint(abs_val);
    uint dec_part = uint(fract(abs_val) * 100.0 + 0.5);
    if (dec_part >= 100u) {
        int_part += 1u;
        dec_part -= 100u;
    }

    uint d0 = extract_digit(int_part, 4u);
    uint d1 = extract_digit(int_part, 3u);
    uint d2 = extract_digit(int_part, 2u);
    uint d3 = extract_digit(int_part, 1u);
    uint d4 = extract_digit(int_part, 0u);
    uint d5 = extract_digit(dec_part, 1u);
    uint d6 = extract_digit(dec_part, 0u);

    // Skip leading zeros
    uint first = 4u;
    if (d0 > 0u) first = 0u;
    else if (d1 > 0u) first = 1u;
    else if (d2 > 0u) first = 2u;
    else if (d3 > 0u) first = 3u;

    vec4 r = vec4(0.0);

    if (negative)    r = max(r, draw_char(CH_MINUS, origin, px, cx));
    if (first <= 0u) r = max(r, draw_char(int(d0) + CH_0, origin, px, cx));
    if (first <= 1u) r = max(r, draw_char(int(d1) + CH_0, origin, px, cx));
    if (first <= 2u) r = max(r, draw_char(int(d2) + CH_0, origin, px, cx));
    if (first <= 3u) r = max(r, draw_char(int(d3) + CH_0, origin, px, cx));
    r = max(r, draw_char(int(d4) + CH_0, origin, px, cx));
    r = max(r, draw_char(CH_DOT, origin, px, cx));
    r = max(r, draw_char(int(d5) + CH_0, origin, px, cx));
    r = max(r, draw_char(int(d6) + CH_0, origin, px, cx));

    return r;
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);
    vec2 px = HOOKED_pos * HOOKED_size;
    vec2 origin = vec2(MARGIN * SCALE, HOOKED_size.y - MARGIN * SCALE - CHAR_H * SCALE);

    float cx = 0.0;
    vec4 r = vec4(0.0);

    r = max(r, draw_char(CH_P, origin, px, cx));
    r = max(r, draw_char(CH_T, origin, px, cx));
    r = max(r, draw_char(CH_S, origin, px, cx));
    r = max(r, draw_char(CH_COLON, origin, px, cx));
    r = max(r, draw_number(PTS, origin, px, cx));

    if (r.a == 0.0)
        r = draw_background(origin, px, cx);

    color.rgb = mix(color.rgb, r.rgb, r.a);
    return color;
}
