export const DEFAULT_PROFILE = {
  name: 'Arman Sharoyan',
  bio: 'Sophomore student at Yerevan State University, specialty in Computer Systems Engineering. Working on Discrete Structures induction tools and compiler syntax checkers.',
  avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150',
  handle: '@asharoyan'
};

export const DEFAULT_POSTS = [
  {
    id: 'post-1',
    authorName: 'Armine Sargsyan',
    authorHandle: '@armine_s',
    authorAvatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=200',
    text: "Armand's mathematical induction proof guidelines for the Discrete Mathematics final have updated. Be sure to check the recursive algorithm structures folder in AI Workspace. I've compiled several syntax translators to help you translate math formulas to Armenian and Russian. Drop a whisper if you want to study together in lab 2A!",
    privacy: 'public',
    timestamp: '2 hours ago',
    likes: 14,
    likedByUser: false
  },
  {
    id: 'post-2',
    authorName: 'Vahan Mkrtchyan',
    authorHandle: '@v_vahan',
    authorAvatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=200',
    text: 'Private release alert: I\'ve successfully deployed a local stark interface mock to bypass database dependency lags in our computer networks class. Only friends verified on the campus network can view this study loop. Contact me directly in Whispers to receive the terminal configuration files!',
    privacy: 'friends',
    timestamp: '4 hours ago',
    likes: 8,
    likedByUser: true
  },
  {
    id: 'post-3',
    authorName: 'Tatev Baghdasaryan',
    authorHandle: '@tatev_b',
    authorAvatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
    text: 'Has anyone finished the Armenian literature research log assigned by Dr. Khachaturian? I am stuck translating several old-spelling documents from early 1900s. AI machine translation is struggling. I\'m waiting in the study hall if anyone wants to collaborate!',
    privacy: 'public',
    timestamp: 'Yesterday',
    likes: 3,
    likedByUser: false
  }
];

export const DEFAULT_DIRECT_MESSAGES = {
  armine: [
    {
      id: 'm1',
      sender: 'peer',
      senderName: 'Armine Sargsyan',
      senderAvatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=200',
      text: 'Hello Arman! Have you checked the recursive algorithms report draft Dr. Harutyunyan assigned? I am working on the induction step verification.',
      timestamp: '16:11'
    },
    {
      id: 'm2',
      sender: 'self',
      senderName: 'Arman Sharoyan',
      senderAvatar: DEFAULT_PROFILE.avatar,
      text: 'Yes, Armine! I ran some experiments inside our local study labs using the syntax helpers. The formula transitions to proper logical forms seem correct. Let me send a screenshot of my workspace log.',
      timestamp: '16:13'
    },
    {
      id: 'm3',
      sender: 'peer',
      senderName: 'Armine Sargsyan',
      senderAvatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=200',
      text: 'Oh great! Let me double check the formulas directly with the induction module. Here is my current working log.',
      timestamp: '16:15',
      attachment: {
        name: 'Induction-Matrix.log',
        url: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=300'
      }
    }
  ],
  vahan: [
    {
      id: 'm4',
      sender: 'peer',
      senderName: 'Vahan Mkrtchyan',
      senderAvatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=200',
      text: 'Hey Arman, standard systems logs in class indicate our local server router has finished compiling the new network templates.',
      timestamp: '14:22'
    },
    {
      id: 'm5',
      sender: 'peer',
      senderName: 'Vahan Mkrtchyan',
      senderAvatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=200',
      text: 'I uploaded the mock schematic file. Please review and let me know if the state definitions align with the Yerevan database specs.',
      timestamp: '14:24'
    }
  ],
  tatev: [
    {
      id: 'm6',
      sender: 'self',
      senderName: 'Arman Sharoyan',
      senderAvatar: DEFAULT_PROFILE.avatar,
      text: 'Hey Tatev, did the Armenian grammar syntax analysis program manage to parse the ancient files? Let know if you are in the lab today.',
      timestamp: 'Yesterday'
    },
    {
      id: 'm7',
      sender: 'peer',
      senderName: 'Tatev Baghdasaryan',
      senderAvatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
      text: 'Sure! Meet me in computer lab 2A. I have several translation transcripts printed for the class bibliography.',
      timestamp: 'Yesterday'
    }
  ]
};

export const DEFAULT_GROUP_MESSAGES = {
  cs101: [
    {
      id: 'g1',
      sender: 'peer',
      senderName: 'Armine Sargsyan',
      senderAvatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=200',
      text: 'Hey everyone! I ran the recursive syntax validator we coded this morning on the Yerevan school database dictionary. It is rejecting strings using compound orthographies. Anyone else getting this error?',
      timestamp: '14:02'
    },
    {
      id: 'g2',
      sender: 'peer',
      senderName: 'Vahan Mkrtchyan',
      senderAvatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=200',
      text: 'Yes, Armine! The regular expression in state 4 is too strict. It treats old Armenian ligature formations. If you update the alphabet map matrix, it compiles successfully.',
      timestamp: '14:05'
    },
    {
      id: 'g3',
      sender: 'self',
      senderName: 'Arman Sharoyan',
      senderAvatar: DEFAULT_PROFILE.avatar,
      text: 'I\'ve added the updated alphabet syntax definitions directly to the AI Core. If you go to the Advisor tab and select Session 02, you can ask for the alphabet array transitions. It outputs the exact matrices verified by Prof. Harutyunyan.',
      timestamp: '14:10'
    }
  ],
  math2a: [
    {
      id: 'g4',
      sender: 'peer',
      senderName: 'Armine Sargsyan',
      senderAvatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=200',
      text: 'Does anyone have the induction proof base case verification steps? I\'m struggling with equation 3B.',
      timestamp: '11:15'
    },
    {
      id: 'g5',
      sender: 'peer',
      senderName: 'Tatev Baghdasaryan',
      senderAvatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
      text: 'I have the printed notes, Armine! For n = 1, the LHS is 1/(1*3) = 1/3, and RHS is 1/(2*1 + 1) = 1/3. The base case holds solidly.',
      timestamp: '11:18'
    }
  ],
  armenian: [
    {
      id: 'g6',
      sender: 'peer',
      senderName: 'Tatev Baghdasaryan',
      senderAvatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
      text: 'I uploaded the ancient-spelling literature translation draft. I configured the inline translation toggles inside AI Core Session 3 for verification.',
      timestamp: 'Yesterday'
    }
  ]
};

export const DEFAULT_AI_MESSAGES = {
  session1: [
    {
      id: 'ai-q1',
      sender: 'self',
      senderName: 'You',
      senderAvatar: '',
      text: 'Prove by mathematical induction that a binary tree of height h has at most 2^h - 1 nodes.',
      timestamp: '15:30'
    },
    {
      id: 'ai-a1',
      sender: 'peer',
      senderName: 'YSCI Core Advisor',
      senderAvatar: '',
      text: '',
      timestamp: '15:31',
      translations: {
        en: `<strong>1. Base Case (h = 1):</strong><p class="mt-1 pl-3 border-l-2 border-[#ff4d00]/30 text-stone-300 light-theme:text-stone-600">A binary tree of height 1 has only a single root node. Substituting h = 1 into the theorem formula: 2^1 - 1 = 1 node. Thus, verified.</p><strong class="block mt-3">2. Inductive Step:</strong><p class="mt-1 pl-3 border-l-2 border-[#ff4d00]/30 text-stone-300 light-theme:text-stone-600">Assume height k has at most 2^k - 1 nodes. A tree of height k+1 consists of a root and at most two subtrees of height k. Total maximum nodes is 1 + 2 * (2^k - 1) = 2^(k+1) - 1. Verification holds. Q.E.D.</p>`,
        ru: `<strong>1. Базовый случай (h = 1):</strong><p class="mt-1 pl-3 border-l-2 border-[#ff4d00]/30 text-stone-300 light-theme:text-stone-600">Двоичное дерево высоты 1 содержит ровно 1 корневой узел. Подставляя h=1 в исходную формулу: 2^1 - 1 = 1. Базовый случай подтвержден.</p><strong class="block mt-3">2. Индукционный переход:</strong><p class="mt-1 pl-3 border-l-2 border-[#ff4d00]/30 text-stone-300 light-theme:text-stone-600">Предположим, что для высоты k дерево содержит не более 2^k - 1 узлов. Демо высоты k+1 содержит корень и макс. два поддерева высоты k. Итого узлов: 1 + 2 * (2^k - 1) = 2^(k+1) - 1. Переход доказан.</p>`,
        am: `<strong>1. Հիմնական դեպք (h = 1):</strong><p class="mt-1 pl-3 border-l-2 border-[#ff4d00]/30 text-stone-300 light-theme:text-stone-600">1 բարձրությամբ երկուական ծառն ունի միայն մեկ արմատային հանգույց: h = 1 արժեքը տեղադրելով բանաձևի մեջ. 2^1 - 1 = 1 հանգույց: Հետևաբար, հիմնական դեպքը հաստատված է:</p><strong class="block mt-3">2. Ինդուկցիոն քայլ:</strong><p class="mt-1 pl-3 border-l-2 border-[#ff4d00]/30 text-stone-300 light-theme:text-stone-600">Ենթադրենք, որ k բարձրությամբ երկուական ծառն ունի առավելագույնը 2^k - 1 հանգույց: k+1 բարձրությամբ ծառը բաղկացած է արմատից և առավելագույնը երկու ենթածառերից: Ընդհանուր հանգույցները՝ 1 + 2 * (2^k - 1) = 2^(k+1) - 1: Ապացուցված է:</p>`
      }
    }
  ],
  session2: [
    {
      id: 'ai-q2',
      sender: 'self',
      senderName: 'You',
      senderAvatar: '',
      text: 'What are the updated alphabet transitions for CS101 assignment 3?',
      timestamp: '14:15'
    },
    {
      id: 'ai-a2',
      sender: 'peer',
      senderName: 'YSCI Core Advisor',
      senderAvatar: '',
      text: '',
      timestamp: '14:16',
      translations: {
        en: `<p>The transition matrices for the Finite State Automaton parsing lexical variables are configured as follows:</p><pre class="bg-sys-input border border-sys-border p-2.5 font-mono text-[10px] text-sys-accent mt-2 rounded">State q0: input [a-zA-Z]   --> State q1\nState q1: input [a-zA-Z0-9] --> State q1\nState q1: input [և]         --> State q3 (Ligature accept)</pre>`,
        ru: `<p>Матрицы переходов конечного автомата для лексического разбора переменных обновлены:</p><pre class="bg-sys-input border border-sys-border p-2.5 font-mono text-[10px] text-sys-accent mt-2 rounded">Состояние q0: ввод [a-zA-Z]    --> q1\nСостояние q1: ввод [a-zA-Z0-9] --> q1\nСостояние q1: ввод [և]         --> q3 (Сжатие лигатуры)</pre>`,
        am: `<p>Լեքսիկական փոփոխականները վերլուծող վերջավոր ավտոմատի անցումային մատրիցները թարմացվել են.</p><pre class="bg-sys-input border border-sys-border p-2.5 font-mono text-[10px] text-sys-accent mt-2 rounded">q0 Վիճակ: մուտք [a-zA-Z]      --> q1\nq1 Վիճակ: մուտք [a-zA-Z0-9]    --> q1\nq1 Վիճակ: մուտք [և]            --> q3 (Կցագրի ընդունում)</pre>`
      }
    }
  ],
  session3: [
    {
      id: 'ai-q3',
      sender: 'self',
      senderName: 'You',
      senderAvatar: '',
      text: 'Explain the ligature \'և\' in Armenian orthography transitions.',
      timestamp: 'Yesterday'
    },
    {
      id: 'ai-a3',
      sender: 'peer',
      senderName: 'YSCI Core Advisor',
      senderAvatar: '',
      text: '',
      timestamp: 'Yesterday',
      translations: {
        en: `<p>The letter 'և' (ev) is a historical ligature combining 'ե' (e) and 'ւ' (v). In classical Armenian orthography, it was printed as separate characters. Soviet spelling reforms of 1922-1940 officially established 'և' as an individual constituent letter in the alphabet.</p>`,
        ru: `<p>Буква 'և' (ев) это историческая лигатура, соединяющая буквы 'ե' (е) и 'ւ' (в). В классической орфографии писались отдельными знаками. Советская реформа правописания (1922-1940) официально закрепила за 'և' статус независимой буквы армянского алфавита.</p>`,
        am: `<p>«և» տառը պատմական կցագիր է, որը համատեղում է «ե» և «ւ» տառերը: Դասական ուղղագրության մեջ այն տպագրվում էր որպես առանձին նիշեր: 1920-1940-ական թվականների ուղղագրական բարեփոխումները հաստատեցին «և»-ը որպես հայերենի այբուբենի ինքնուրույն և լիիրավ տառ:</p>`
      }
    }
  ]
};
