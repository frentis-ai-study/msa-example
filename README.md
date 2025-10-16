# MSA Shop Application - DDD 리팩토링 학습 프로젝트

> Spring Boot, JPA, DDD 패턴을 활용한 마이크로서비스 아키텍처 학습 프로젝트

## 📚 프로젝트 개요

이 프로젝트는 전통적인 3계층 아키텍처에서 시작하여 **Domain-Driven Design(DDD)** 패턴을 적용하는 과정을 학습하기 위한 교육용 프로젝트입니다.

### 핵심 도메인

- **Order (주문)**: 고객의 상품 주문
- **Inventory (재고)**: 상품 재고 관리
- **Delivery (배송)**: 주문에 대한 배송 처리

### 비즈니스 규칙

```
주문 생성 시:
  1. 재고 검증 (상품 존재 여부, 재고 충분 여부)
  2. 재고 감소
  3. 배송 건 자동 생성
```

---

## 🏗️ 아키텍처 진화 과정

### 1단계: 전통적인 3계층 아키텍처

```
Controller → Service → Repository → Entity
```

**구조:**
```java
OrderController
  └─ OrderService
      └─ OrderRepository
          └─ Order (빈약한 도메인 모델)
```

**특징:**
- ✅ 이해하기 쉬운 구조
- ❌ 빈약한 도메인 모델 (Anemic Domain Model)
- ❌ 비즈니스 로직이 Service에 집중

**코드 예시:**
```java
@Service
public class OrderService {
    public Order save(Order order) {
        return orderRepository.save(order);
    }
}
```

---

### 2단계: 비즈니스 로직 통합

**변경점:**
- OrderService에 주문 생성/수정 로직 분리
- 재고 감소 + 배송 생성 자동화

**코드:**
```java
@Service
public class OrderService {
    @Transactional
    public Order createOrder(Order order) {
        // 1. 재고 검증 및 감소
        Inventory inventory = inventoryService.findById(productId);
        inventory.setStock(inventory.getStock() - order.getQty());
        inventoryService.save(inventory);

        // 2. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 3. 배송 생성
        deliveryService.createDelivery(...);

        return savedOrder;
    }
}
```

**문제점:**
- 여전히 도메인 객체는 데이터 컨테이너에 불과
- 비즈니스 로직이 Service에 흩어짐

---

### 3단계: DDD 패턴 적용 시도

**목표:**
- 도메인 객체가 자신의 비즈니스 로직 관리
- Service/Controller 레이어 제거
- Spring Data REST 활용

**변경점:**
```java
// OrderService, OrderController 삭제
// RepositoryRestResource 적용

@RepositoryRestResource(path = "orders")
public interface OrderRepository extends JpaRepository<Order, Long> {
}

@Entity
public class Order {
    @PostPersist
    public void postPersist() {
        // 재고 감소
        // 배송 생성
    }
}
```

**장점:**
- ✅ 도메인이 비즈니스 로직 캡슐화
- ✅ 코드 간소화 (~200 라인 감소)
- ✅ REST API 자동 생성

**문제 발생:**
```
ConcurrentModificationException!
```

---

## 🚨 JPA 생명주기 충돌 문제

### 문제 상황

```java
@PostPersist
public void postPersist() {
    inventoryRepository.save(inventory);  // ❌
    deliveryRepository.save(delivery);    // ❌
}
```

### 왜 문제가 발생하는가?

```
트랜잭션 시작
  ├─ Order INSERT 준비
  ├─ Hibernate ActionQueue에 작업 등록
  ├─ ActionQueue.executeActions() 시작 ← Iterator 생성
  │   ├─ Order INSERT 실행
  │   ├─ @PostPersist 콜백 실행
  │   │   ├─ inventoryRepository.save() 호출
  │   │   │   └─ 같은 ActionQueue에 UPDATE 추가 시도
  │   │   │       └─ ❌ Iterator 순회 중 리스트 수정!
  │   │   └─ ConcurrentModificationException 발생
```

**핵심 문제:**
- `@PostPersist`는 INSERT 후지만, 여전히 **Hibernate flush 중**
- 같은 트랜잭션, 같은 EntityManager, 같은 ActionQueue
- Repository 작업이 ActionQueue를 동시 수정 시도

### 시도했던 해결 방법들

| 방법 | 시도 내용 | 결과 |
|------|-----------|------|
| 1 | `@PrePersist` 사용 | ❌ 여전히 flush 중 |
| 2 | `save()` 제거, dirty checking만 사용 | ❌ 배송 생성 불가 |
| 3 | Domain Events 패턴 | ✅ 가능하지만 복잡 |
| 4 | **Service + REQUIRES_NEW** | ✅ **최종 선택** |

---

## ✅ 최종 해결: 하이브리드 아키텍처

### 핵심 아이디어

> "필요한 곳에만 Service를 사용하자"

**참고 코드에서 발견한 패턴:**
```java
@PostPersist
public void onPostPersist() {
    InventoryService.decreaseStock()  // ← Service 메서드 (REQUIRES_NEW)
}
```

**왜 동작하는가?**
- Service 메서드가 **별도 트랜잭션**으로 실행
- 새로운 EntityManager, 새로운 ActionQueue 사용
- JPA 충돌 없음!

### 최종 구조

```
Order (Aggregate Root)
├─ @RepositoryRestResource ✅
├─ OrderService ❌ (DDD 유지)
└─ @PostPersist
    ├─ InventoryService.decreaseStock() (REQUIRES_NEW)
    └─ DeliveryService.createDelivery() (REQUIRES_NEW)

Inventory
├─ @RepositoryRestResource ✅
└─ InventoryService ✅ (트랜잭션 분리용)

Delivery
├─ @RepositoryRestResource ✅
└─ DeliveryService ✅ (트랜잭션 분리용)
```

### 구현 코드

**Order (도메인 객체가 비즈니스 로직 제어)**
```java
@Entity
@Table(name = "shop-order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String productId;
    private Integer qty;
    private String customerId;
    private String address;
    private String status;

    @PostPersist
    public void postPersist() {
        // ApplicationContext에서 Service 빈 가져오기
        InventoryService inventoryService =
            ApplicationContextProvider.getBean(InventoryService.class);
        DeliveryService deliveryService =
            ApplicationContextProvider.getBean(DeliveryService.class);

        // 1. 재고 감소 (별도 트랜잭션)
        Long productId = Long.parseLong(this.productId);
        inventoryService.decreaseStock(productId, this.qty);

        // 2. 배송 생성 (별도 트랜잭션)
        deliveryService.createDelivery(
            this.id, this.customerId, this.qty, this.address
        );
    }
}
```

**InventoryService (REQUIRES_NEW)**
```java
@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long productId, Integer qty) {
        // 새로운 트랜잭션 시작
        // 새로운 EntityManager 사용
        // 새로운 ActionQueue 사용

        Inventory inventory = inventoryRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException(
                "상품을 찾을 수 없습니다: " + productId));

        if (inventory.getStock() < qty) {
            throw new IllegalStateException(
                "재고가 부족합니다. 현재 재고: " + inventory.getStock());
        }

        inventory.setStock(inventory.getStock() - qty);
        inventoryRepository.save(inventory);
    }
}
```

**DeliveryService (REQUIRES_NEW)**
```java
@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDelivery(Long orderId, String customerId,
                               Integer qty, String address) {
        // 새로운 트랜잭션으로 실행
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setCustomerId(customerId);
        delivery.setQty(qty);
        delivery.setAddress(address);
        delivery.setStatus("PENDING");

        deliveryRepository.save(delivery);
    }
}
```

### 트랜잭션 흐름

```
트랜잭션 A (Order 저장)
  ├─ Order INSERT
  ├─ @PostPersist 실행
  │   ├─ inventoryService.decreaseStock()
  │   │   └─ 트랜잭션 B (REQUIRES_NEW) ✅
  │   │       ├─ 새로운 EntityManager
  │   │       ├─ 새로운 ActionQueue
  │   │       └─ Inventory UPDATE
  │   │
  │   └─ deliveryService.createDelivery()
  │       └─ 트랜잭션 C (REQUIRES_NEW) ✅
  │           ├─ 새로운 EntityManager
  │           ├─ 새로운 ActionQueue
  │           └─ Delivery INSERT
  │
  └─ 트랜잭션 A 커밋
```

---

## 🎯 핵심 학습 포인트

### 1. DDD vs 현실적 제약

**이상:**
- 도메인 객체가 모든 비즈니스 로직 관리
- Service 레이어 불필요

**현실:**
- JPA 생명주기 제약
- 트랜잭션 관리 필요
- **실용적 절충안이 필요**

### 2. @PrePersist vs @PostPersist

| 시점 | @PrePersist | @PostPersist |
|------|-------------|--------------|
| 실행 시점 | INSERT 전 | INSERT 후 |
| ID 존재 | ❌ | ✅ |
| 트랜잭션 순서 | 재고 감소 → Order 생성 | Order 생성 → 재고 감소 |
| 실패 시 | 재고만 감소 가능 | Order는 이미 저장됨 |
| 추천도 | ⚠️ 위험 | ✅ 안전 |

### 3. REQUIRES_NEW의 중요성

```java
// ❌ 기본 트랜잭션 (REQUIRED)
@Transactional
public void decreaseStock() {
    // 부모 트랜잭션에 참여
    // 같은 EntityManager 사용
    // ActionQueue 충돌!
}

// ✅ 새로운 트랜잭션 (REQUIRES_NEW)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void decreaseStock() {
    // 완전히 새로운 트랜잭션
    // 새로운 EntityManager
    // 새로운 ActionQueue
    // 충돌 없음!
}
```

### 4. ApplicationContextProvider 패턴

**왜 필요한가?**
- JPA Entity는 Spring Bean이 아님
- `@Autowired` 불가능
- ApplicationContext에서 직접 빈 가져와야 함

**구현:**
```java
@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}
```

---

## 🚀 실행 방법

### 요구사항

- Java 17+
- Gradle

### 실행

```bash
./gradlew bootRun
```

### H2 Console 접속

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:shopdb`
- Username: `sa`
- Password: (공백)

---

## 📡 API 사용 예제

### 1. 재고 등록

```bash
POST http://localhost:8080/inventories
Content-Type: application/json

{
  "name": "노트북",
  "stock": 100,
  "price": 1500000
}
```

### 2. 주문 생성

```bash
POST http://localhost:8080/orders
Content-Type: application/json

{
  "productId": "1",
  "qty": 5,
  "customerId": "customer-001",
  "address": "서울시 강남구",
  "status": "ORDERED"
}
```

**자동으로 발생하는 일:**
1. ✅ 재고 검증 (ID=1 상품 존재 여부)
2. ✅ 재고 감소 (100 → 95)
3. ✅ 배송 건 생성 (status: PENDING)

### 3. 전체 조회

```bash
# 주문 목록
GET http://localhost:8080/orders

# 재고 목록
GET http://localhost:8080/inventories

# 배송 목록
GET http://localhost:8080/deliveries
```

### 4. Spring Data REST 기능

```bash
# HAL Browser
GET http://localhost:8080/

# 페이징
GET http://localhost:8080/orders?page=0&size=10

# 정렬
GET http://localhost:8080/orders?sort=id,desc

# 검색 (Repository에 메서드 추가 시)
GET http://localhost:8080/orders/search/findByCustomerId?customerId=customer-001
```

---

## 💡 트러블슈팅 가이드

### ConcurrentModificationException 발생 시

**증상:**
```
java.util.ConcurrentModificationException
  at org.hibernate.engine.spi.ActionQueue.executeActions
```

**원인:**
- JPA 생명주기(`@PrePersist`, `@PostPersist`) 내에서 Repository 직접 호출

**해결:**
- Service 메서드 사용
- `@Transactional(propagation = Propagation.REQUIRES_NEW)` 적용

### 재고가 감소하지 않는 경우

**확인사항:**
1. InventoryService에 `@Transactional` 있는지 확인
2. `REQUIRES_NEW` propagation 설정 확인
3. ApplicationContextProvider가 Bean으로 등록되었는지 확인

### 배송 건이 생성되지 않는 경우

**확인사항:**
1. Order.postPersist() 실행되는지 로그 확인
2. DeliveryService가 정상 호출되는지 확인
3. 트랜잭션 롤백 여부 확인

---

## 📊 아키텍처 비교

| 항목 | 전통 3계층 | DDD (순수) | 하이브리드 (최종) |
|------|-----------|-----------|------------------|
| DDD 원칙 | ❌ | ✅ | ✅ |
| 코드 간소화 | ❌ | ✅ | ⚠️ |
| 트랜잭션 안정성 | ✅ | ❌ | ✅ |
| JPA 충돌 | ✅ | ❌ | ✅ |
| 확장성 | ⚠️ | ✅ | ✅ |
| 학습 곡선 | 낮음 | 높음 | 중간 |
| **추천도** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 📖 참고 자료

- [Spring Data REST Reference](https://docs.spring.io/spring-data/rest/docs/current/reference/html/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [JPA Entity Lifecycle](https://www.baeldung.com/jpa-entity-lifecycle-events)
- [Spring Transaction Propagation](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#tx-propagation)

---

## 🏷️ Git Tags

프로젝트의 진화 과정을 태그로 표시했습니다:

```bash
# 레거시 버전 (MSA 분리 전)
git checkout legacy

# 각 리팩토링 단계
git log --oneline --decorate
```

---

## 👥 기여

이 프로젝트는 DDD 학습을 위한 교육용 프로젝트입니다.

---

## 📝 라이선스

MIT License
