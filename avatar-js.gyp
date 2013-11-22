{
    'includes': {
        'config.gypi',
    },
    'variables': {
        'AVATAR_JS_HOME%': '<(AVATAR_JS_HOME%)',
        'SRC%': '<(AVATAR_JS_HOME%)/out/<(target)/obj.target/avatar-js',
    },
    'target_defaults': {
        'default_configuration': '<(target)',
        'configurations': {
            'Debug': {
                'defines': [ 'DEBUG', '_DEBUG' ],
                'cflags': [ '-g', '-O0' ],
                'conditions': [
                    ['OS == "win"', {
                        'msvs_settings': {
                            'VCCLCompilerTool': {
                                'ObjectFile': 'out\<(target)\obj.target\\avatar-js\\',
                            },
                            'VCLinkerTool': {
                                'GenerateDebugInformation': 'true',
                            },
                        },
                    }],
                    [ 'OS=="linux"', {
                    }],
                    [ 'OS=="mac"', {
                    }],
                ],
            },
            'Release': {
                'defines': [ 'NDEBUG' ],
                'conditions': [
                    ['OS == "win"', {
                    }],
                    [ 'OS=="linux"', {
                    }],
                    [ 'OS=="mac"', {
                    }],
                ],
            },
        }
    },
    'targets': [
        {
            'target_name': 'avatar-js',
            'type': 'shared_library',
            'defines': [
            ],
            'dependencies': [
            ],
            'include_dirs': [
                '<(JAVA_HOME)/include',
            ],
            'conditions': [
                ['OS == "linux"', {
                    'sources': [
                        'os.cpp',
                        'process.cpp',
                        'throw.cpp',
                    ],
                    'libraries': [
                    ],
                    'defines': [
                        '__POSIX__',
                    ],
                    'cflags': [
                        '-fPIC',
                    ],
                    'ldflags': [
                    ],
                    'include_dirs': [
                        '<(JAVA_HOME)/include/linux',
                    ],
                }],
                ['OS == "mac"', {
                    'target_conditions': [
                        ['target_arch=="x64"', {
                            'xcode_settings': {'ARCHS': ['x86_64']},
                        }]
                    ],
                    'sources': [
                        '<(SRC)/os.cpp',
                        '<(SRC)/process.cpp',
                        '<(SRC)/throw.cpp',
                    ],
                    'libraries': [
                    ],
                    'defines': [
                        '__POSIX__',
                    ],
                    'include_dirs': [
                        '<(JAVA_HOME)/include/darwin',
                    ],
                }],
                ['OS == "win"', {
                    'target_conditions': [
                        ['target_arch=="x64"', {
                            'msvs_configuration_platform': 'x64'
                        }]
                    ],
                    'sources': [
                        '<(SRC)/os.cpp',
                        '<(SRC)/process.cpp',
                        '<(SRC)/throw.cpp',
                    ],
                    'defines': [
                        '_UNICODE',
                        'UNICODE',
                        '_WIN32_WINNT=0x0600',
                        '_WIN32',
                    ],
                    'cflags': [
                    ],
                    'ldflags': [
                    ],
                    'include_dirs': [
                        '<(JAVA_HOME)/include/win32',
                    ],
                    'libraries': [
                    ],
                }]
            ],
        },

    ],

}
