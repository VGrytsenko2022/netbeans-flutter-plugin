import 'dart:typed_data';

/// Small dependency-free SHA-256 implementation used by the NBFC transport.
Uint8List sha256Bytes(List<int> input) {
  final bitLength = input.length * 8;
  final paddedLength = ((input.length + 9 + 63) ~/ 64) * 64;
  final bytes = Uint8List(paddedLength)..setRange(0, input.length, input);
  bytes[input.length] = 0x80;
  for (var index = 0; index < 8; index++) {
    bytes[paddedLength - 1 - index] = (bitLength >> (8 * index)) & 0xff;
  }

  var h0 = 0x6a09e667;
  var h1 = 0xbb67ae85;
  var h2 = 0x3c6ef372;
  var h3 = 0xa54ff53a;
  var h4 = 0x510e527f;
  var h5 = 0x9b05688c;
  var h6 = 0x1f83d9ab;
  var h7 = 0x5be0cd19;
  final words = Uint32List(64);

  for (var offset = 0; offset < bytes.length; offset += 64) {
    for (var index = 0; index < 16; index++) {
      final wordOffset = offset + index * 4;
      words[index] =
          (bytes[wordOffset] << 24) |
          (bytes[wordOffset + 1] << 16) |
          (bytes[wordOffset + 2] << 8) |
          bytes[wordOffset + 3];
    }
    for (var index = 16; index < 64; index++) {
      final x = words[index - 15];
      final y = words[index - 2];
      final s0 = _rotateRight(x, 7) ^ _rotateRight(x, 18) ^ (x >>> 3);
      final s1 = _rotateRight(y, 17) ^ _rotateRight(y, 19) ^ (y >>> 10);
      words[index] = _u32(words[index - 16] + s0 + words[index - 7] + s1);
    }

    var a = h0;
    var b = h1;
    var c = h2;
    var d = h3;
    var e = h4;
    var f = h5;
    var g = h6;
    var h = h7;
    for (var index = 0; index < 64; index++) {
      final sum1 =
          _rotateRight(e, 6) ^ _rotateRight(e, 11) ^ _rotateRight(e, 25);
      final choice = (e & f) ^ ((~e) & g);
      final temp1 = _u32(h + sum1 + choice + _constants[index] + words[index]);
      final sum0 =
          _rotateRight(a, 2) ^ _rotateRight(a, 13) ^ _rotateRight(a, 22);
      final majority = (a & b) ^ (a & c) ^ (b & c);
      final temp2 = _u32(sum0 + majority);
      h = g;
      g = f;
      f = e;
      e = _u32(d + temp1);
      d = c;
      c = b;
      b = a;
      a = _u32(temp1 + temp2);
    }

    h0 = _u32(h0 + a);
    h1 = _u32(h1 + b);
    h2 = _u32(h2 + c);
    h3 = _u32(h3 + d);
    h4 = _u32(h4 + e);
    h5 = _u32(h5 + f);
    h6 = _u32(h6 + g);
    h7 = _u32(h7 + h);
  }

  final digest = ByteData(32);
  final hashes = [h0, h1, h2, h3, h4, h5, h6, h7];
  for (var index = 0; index < hashes.length; index++) {
    digest.setUint32(index * 4, hashes[index], Endian.big);
  }
  return digest.buffer.asUint8List();
}

String sha256Hex(List<int> input) => sha256Bytes(
  input,
).map((byte) => byte.toRadixString(16).padLeft(2, '0')).join();

int _rotateRight(int value, int count) =>
    _u32((value >>> count) | (value << (32 - count)));

int _u32(int value) => value & 0xffffffff;

const _constants = <int>[
  0x428a2f98,
  0x71374491,
  0xb5c0fbcf,
  0xe9b5dba5,
  0x3956c25b,
  0x59f111f1,
  0x923f82a4,
  0xab1c5ed5,
  0xd807aa98,
  0x12835b01,
  0x243185be,
  0x550c7dc3,
  0x72be5d74,
  0x80deb1fe,
  0x9bdc06a7,
  0xc19bf174,
  0xe49b69c1,
  0xefbe4786,
  0x0fc19dc6,
  0x240ca1cc,
  0x2de92c6f,
  0x4a7484aa,
  0x5cb0a9dc,
  0x76f988da,
  0x983e5152,
  0xa831c66d,
  0xb00327c8,
  0xbf597fc7,
  0xc6e00bf3,
  0xd5a79147,
  0x06ca6351,
  0x14292967,
  0x27b70a85,
  0x2e1b2138,
  0x4d2c6dfc,
  0x53380d13,
  0x650a7354,
  0x766a0abb,
  0x81c2c92e,
  0x92722c85,
  0xa2bfe8a1,
  0xa81a664b,
  0xc24b8b70,
  0xc76c51a3,
  0xd192e819,
  0xd6990624,
  0xf40e3585,
  0x106aa070,
  0x19a4c116,
  0x1e376c08,
  0x2748774c,
  0x34b0bcb5,
  0x391c0cb3,
  0x4ed8aa4a,
  0x5b9cca4f,
  0x682e6ff3,
  0x748f82ee,
  0x78a5636f,
  0x84c87814,
  0x8cc70208,
  0x90befffa,
  0xa4506ceb,
  0xbef9a3f7,
  0xc67178f2,
];
