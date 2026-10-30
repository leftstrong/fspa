/* 良物换物 · 官网交互
   无依赖，全部原生实现：吸顶导航、滚动揭示、数字滚动、进度条、手风琴 */
(function () {
  'use strict';

  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------------- 导航 ---------------- */
  var nav = document.querySelector('.nav');
  var toggle = document.querySelector('.nav__toggle');
  var progress = document.querySelector('.scroll-progress');

  function onScroll() {
    var y = window.scrollY || document.documentElement.scrollTop;
    if (nav) nav.classList.toggle('is-stuck', y > 24);
    if (progress) {
      var h = document.documentElement.scrollHeight - window.innerHeight;
      progress.style.width = (h > 0 ? (y / h) * 100 : 0) + '%';
    }
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  if (toggle && nav) {
    toggle.addEventListener('click', function () {
      nav.classList.toggle('is-open');
      document.body.style.overflow = nav.classList.contains('is-open') ? 'hidden' : '';
    });
    nav.querySelectorAll('.nav__mobile a').forEach(function (a) {
      a.addEventListener('click', function () {
        nav.classList.remove('is-open');
        document.body.style.overflow = '';
      });
    });
  }

  /* ---------------- 当前页高亮 ---------------- */
  var path = (location.pathname.split('/').pop() || 'index.html').toLowerCase();
  document.querySelectorAll('[data-page]').forEach(function (el) {
    var target = (el.getAttribute('data-page') || '').toLowerCase();
    if (target === path || (path === '' && target === 'index.html')) {
      el.classList.add('is-active');
    }
  });

  /* ---------------- 滚动揭示 ---------------- */
  var reveals = Array.prototype.slice.call(document.querySelectorAll('.reveal'));

  // 同一容器内的兄弟节点依次入场
  reveals.forEach(function (el) {
    var group = el.parentElement ? el.parentElement.querySelectorAll(':scope > .reveal') : [];
    if (group.length > 1) {
      var idx = Array.prototype.indexOf.call(group, el);
      if (!el.style.getPropertyValue('--d')) {
        el.style.setProperty('--d', Math.min(idx, 5) * 90 + 'ms');
      }
    }
  });

  if (!('IntersectionObserver' in window) || reduceMotion) {
    reveals.forEach(function (el) { el.classList.add('is-in'); });
  } else {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-in');
          io.unobserve(entry.target);
        }
      });
    }, { rootMargin: '0px 0px -12% 0px', threshold: 0.12 });
    reveals.forEach(function (el) { io.observe(el); });
  }

  /* ---------------- 数字滚动 ---------------- */
  function animateCount(el) {
    var to = parseFloat(el.getAttribute('data-count'));
    var suffix = el.getAttribute('data-suffix') || '';
    var decimals = parseInt(el.getAttribute('data-decimals') || '0', 10);
    if (isNaN(to)) return;
    if (reduceMotion) {
      el.textContent = to.toFixed(decimals) + suffix;
      return;
    }
    var from = 0;
    var dur = 1400;
    var start = null;
    function frame(ts) {
      if (start === null) start = ts;
      var p = Math.min((ts - start) / dur, 1);
      var eased = 1 - Math.pow(1 - p, 3);
      var val = from + (to - from) * eased;
      el.textContent = val.toFixed(decimals) + suffix;
      if (p < 1) requestAnimationFrame(frame);
      else el.textContent = to.toFixed(decimals) + suffix;
    }
    requestAnimationFrame(frame);
  }

  var counters = document.querySelectorAll('[data-count]');
  if (counters.length) {
    if (!('IntersectionObserver' in window)) {
      counters.forEach(animateCount);
    } else {
      var cio = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            animateCount(entry.target);
            cio.unobserve(entry.target);
          }
        });
      }, { threshold: 0.4 });
      counters.forEach(function (el) { cio.observe(el); });
    }
  }

  /* ---------------- FAQ：同时只展开一项 ---------------- */
  var faqItems = document.querySelectorAll('.faq__item');
  faqItems.forEach(function (item) {
    item.addEventListener('toggle', function () {
      if (!item.open) return;
      faqItems.forEach(function (other) {
        if (other !== item) other.open = false;
      });
    });
  });

  /* ---------------- 页脚年份 ---------------- */
  document.querySelectorAll('[data-year]').forEach(function (el) {
    el.textContent = new Date().getFullYear();
  });
})();
