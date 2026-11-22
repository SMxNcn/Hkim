#version 330

uniform sampler2D Sampler0;

layout(std140) uniform BlurConfig {
    vec4 Info;
    vec4 Rect;
    vec4 Params;
    vec4 Tint;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texelStep = Info.zw * Params.yz;
    float radius = max(Params.x, 0.5);
    float taps = min(12.0, ceil(radius));
    float stepScale = radius / taps;
    float sigma = max(radius * 0.5, 0.5);
    vec4 sum = texture(Sampler0, texCoord);
    float weight = 1.0;
    for (int i = 1; i <= 12; i++) {
        if (float(i) > taps) break;
        float offset = (float(i) - 0.5) * stepScale;
        float w = exp(-0.5 * offset * offset / (sigma * sigma));
        sum += texture(Sampler0, texCoord + texelStep * offset) * w;
        sum += texture(Sampler0, texCoord - texelStep * offset) * w;
        weight += w * 2.0;
    }
    fragColor = sum / weight;
    fragColor.a = 1.0;
}
